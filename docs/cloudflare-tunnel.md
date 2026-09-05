# Cloudflare tunnel — Thykra

Thykra runs behind its own Cloudflare tunnel, deliberately separate from the
media stack's tunnel on the same host so the two can be routed, restarted and
revoked independently.

| | |
|---|---|
| Tunnel | `95ad6aab-b079-44c9-871d-31ec8f74582c` |
| Container | `thykra-cloudflared`, a service in `docker-compose.unraid.yml` |
| Mode | Token — ingress lives in the Zero Trust dashboard, not in this repo |

## Ingress

The connector joins the stack network, so it targets service names and does not
depend on which host ports are published.

| Hostname | Service |
|---|---|
| `thykra.com` | `http://web:80` |
| `api.thykra.com` | `http://server:8081` |
| `s3.thykra.com` | `http://minio:9000` |

Universal SSL covers `thykra.com` and `*.thykra.com`, so the apex and both
first-level names are certificate-covered with nothing extra. A deeper name such
as `a.b.thykra.com` would need Advanced Certificate Manager.

## Environment changes

| Key | Status | Value |
|---|---|---|
| `CLOUDFLARE_TUNNEL_TOKEN` | new | connector token for the tunnel above |
| `S3_PUBLIC_ENDPOINT` | new | `https://s3.thykra.com` |
| `MINIO_CORS_ALLOW_ORIGIN` | changed | was `*`, now `https://thykra.com,http://tower.local:8088,http://192.168.1.5:8088` |

The token is declared `:?` in compose, so a deploy that forgets it fails
immediately with a named error instead of starting a connector that
authenticates against nothing.

`S3_PUBLIC_ENDPOINT` had to become its own key because `PUBLIC_HOST` was doing
two jobs — it built the S3 URLs *and* fed the `extra_hosts` entry that maps a
name to `192.168.1.5`. Pointing it at the public hostname would have sent the
server to port 443 on the Unraid box, where nothing listens.

```yaml
# before
S3_ENDPOINT: http://${PUBLIC_HOST:-tower.local}:${MINIO_API_PORT:-9000}

# after
S3_ENDPOINT: ${S3_PUBLIC_ENDPOINT:?S3_PUBLIC_ENDPOINT is required}
S3_PUBLIC_BASE_URL: ${S3_PUBLIC_ENDPOINT}/${S3_BUCKET:-thykra-media}
```

The CORS list keeps both LAN forms alongside the public origin, so uploads still
work from a browser on the local network.

## Files touched

- `docker-compose.unraid.yml` — added the `cloudflared` service; `S3_ENDPOINT`
  and `S3_PUBLIC_BASE_URL` now derive from `S3_PUBLIC_ENDPOINT` rather than
  `PUBLIC_HOST`; the `extra_hosts` comment updated to match.
- `.env.unraid` — the three keys above (gitignored; the token lives only here).
- `.env.unraid.example` — same keys documented, with LAN-only defaults.
- `Unraid-Runbook.md` (ops repo) — §1 gains the container row, constraint #3
  rewritten around `S3_PUBLIC_ENDPOINT`, §8 OAuth item now points at
  `https://thykra.com`.

## The size ceiling

`S3StorageService` takes a single `endpoint` and hands it to two consumers: the
SigV4 presigner and its own `S3Client`. The presigner has to carry the public
origin, because SigV4 signs the Host header and the URL cannot be rewritten
afterwards. The client inherits that same origin — so the server's own
`putObject`, `getObjectAsBytes` and `deleteObject` calls leave the host and come
back through Cloudflare:

```
thykra-server → Cloudflare edge → thykra-cloudflared → minio:9000
```

That path is capped by Cloudflare's request body limit, and is dead whenever the
tunnel is.

| Plan | Max body size |
|---|---|
| Free | 100 MB |
| Pro | 100 MB |
| Business | 200 MB |
| Enterprise | 500 MB, raisable on request |

Over the limit returns `413 Request entity too large` from Cloudflare — nothing
appears in MinIO's log. Photographs pass comfortably; video does not.

### The fix, when it bites

Split the two consumers: give `S3StorageService` a second constructor parameter
used only by the presigner, and let the internal client keep `http://minio:9000`.
Roughly ten lines across `S3StorageService.kt` and `Application.kt`, plus one
new key.

```env
S3_PUBLIC_ENDPOINT=https://s3.thykra.com  # presigner — signed, public
S3_INTERNAL_ENDPOINT=http://minio:9000    # client — direct, in-network
```

**What the split does not fix:** browsers and phones still upload through the
tunnel, so their PUTs stay under the same ceiling. Only the server's own traffic
comes back inside the house.

## Deploy checklist

1. Delete the stale `Thykra.292916.xyz` public hostname from the media tunnel.
2. Add the three hostnames above to tunnel `95ad6aab-…` (all type HTTP).
3. `bash infra/deploy-unraid.sh` from the repo root.
4. Authorize `https://thykra.com` as a JavaScript origin on the Google OAuth
   web client, or web sign-in fails.
5. Verify: `ssh root@tower.local "docker logs thykra-cloudflared --tail 20"` —
   expect four `Registered tunnel connection` lines and one
   `Updated to new configuration` listing the three rules.
