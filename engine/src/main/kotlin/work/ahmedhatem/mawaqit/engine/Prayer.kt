package work.ahmedhatem.mawaqit.engine

/**
 * The six daily times shown by the app, in the order they occur during a day.
 * Sunrise is not a prayer, but it ends the Fajr time and starts the forbidden period, so it is listed.
 */
enum class Prayer(val arabicName: String) {
    FAJR("الفجر"),
    SUNRISE("الشروق"),
    DHUHR("الظهر"),
    ASR("العصر"),
    MAGHRIB("المغرب"),
    ISHA("العشاء"),
}
