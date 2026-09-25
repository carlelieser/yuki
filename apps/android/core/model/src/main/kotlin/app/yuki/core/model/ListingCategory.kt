package app.yuki.core.model

enum class ListingCategory(val wireValue: String) {
    SystemTweaks("system_tweaks"),
    AppManagement("app_management"),
    FileManagement("file_management"),
    Media("media"),
    Gaming("gaming"),
    Automation("automation"),
    Networking("networking"),
    PrivacySecurity("privacy_security"),
    DeveloperTools("developer_tools"),
    DeviceSpecific("device_specific"),
    Customization("customization"),
    Connectivity("connectivity"),
    Utilities("utilities"),
}

fun readListingCategory(raw: String?): ListingCategory? =
    ListingCategory.entries.firstOrNull { category -> category.wireValue == raw }
