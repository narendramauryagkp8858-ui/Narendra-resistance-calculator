package com.example.model

enum class BandCount(
    val count: Int,
    val roles: List<BandRole>,
    val defaultBands: List<ResistorColor>
) {
    THREE(
        count = 3,
        roles = listOf(BandRole.DIGIT_1, BandRole.DIGIT_2, BandRole.MULTIPLIER),
        defaultBands = listOf(ResistorColor.BROWN, ResistorColor.BLACK, ResistorColor.RED)
    ),
    FOUR(
        count = 4,
        roles = listOf(BandRole.DIGIT_1, BandRole.DIGIT_2, BandRole.MULTIPLIER, BandRole.TOLERANCE),
        defaultBands = listOf(
            ResistorColor.BROWN,
            ResistorColor.BLACK,
            ResistorColor.RED,
            ResistorColor.GOLD
        )
    ),
    FIVE(
        count = 5,
        roles = listOf(
            BandRole.DIGIT_1,
            BandRole.DIGIT_2,
            BandRole.DIGIT_3,
            BandRole.MULTIPLIER,
            BandRole.TOLERANCE
        ),
        defaultBands = listOf(
            ResistorColor.BROWN,
            ResistorColor.BLACK,
            ResistorColor.BLACK,
            ResistorColor.BROWN,
            ResistorColor.BROWN
        )
    ),
    SIX(
        count = 6,
        roles = listOf(
            BandRole.DIGIT_1,
            BandRole.DIGIT_2,
            BandRole.DIGIT_3,
            BandRole.MULTIPLIER,
            BandRole.TOLERANCE,
            BandRole.TEMP_COEFF
        ),
        defaultBands = listOf(
            ResistorColor.BROWN,
            ResistorColor.BLACK,
            ResistorColor.BLACK,
            ResistorColor.BROWN,
            ResistorColor.BROWN,
            ResistorColor.RED
        )
    )
}
