# libtorrent4j
-keep class org.libtorrent4j.** { *; }
-keep class com.frostwire.jlibtorrent.** { *; }
-dontwarn org.libtorrent4j.**

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Media3
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Coroutines
-dontwarn kotlinx.coroutines.**
