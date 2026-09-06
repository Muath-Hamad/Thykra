package com.jameeli.thykra.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Chrome soft, content square.
 *
 * M3 reaches for the wrong slot in four places, so the kit overrides them once:
 * Button -> [Shapes.small] (M3 defaults to a pill), FilledTextField -> small on all four
 * corners, ModalBottomSheet -> [Shapes.large] top corners (M3 defaults to 28), AlertDialog
 * -> large, Snackbar -> medium. FAB and the nav indicator keep the pill; Card keeps medium
 * and chips keep small, which are already right.
 */
val thykraShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/**
 * Every photograph. 0 dp on every edge that is not the parent card's own clip — a card
 * clips at 14 dp and the plate is drawn square within it.
 */
val PlateShape: Shape = RectangleShape

/** Nav indicator, FAB, viewer chrome pills, the "+N" avatar overflow. */
val PillShape: Shape = RoundedCornerShape(percent = 50)
