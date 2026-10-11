package ir.farimod.app

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
