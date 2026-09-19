# AlamootPlus - Android TV
RTSP/ONVIF camera viewer for Android TV.

Features:
- Media3 ExoPlayer with RTSP support
- WS-Discovery ONVIF discovery (UDP 239.255.255.250:3702)
- Brand presets (assets/brand_presets.json)
- Local HTTP webserver on port 8080 (/play?u=<url> remote control)

Build:
1. Open in Android Studio (AGP 8.2.2, Gradle 8.2, JDK 17).
2. gradle-wrapper.jar is a placeholder; run once 'gradle wrapper --gradle-version 8.2'
   or let Android Studio regenerate it, then build normally.
