# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**
# WorkManager workers are instantiated reflectively
-keep class * extends androidx.work.ListenableWorker { <init>(...); }
# ML Kit
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**
