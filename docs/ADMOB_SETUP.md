# AdMob integration — 0.2.0

## Build requirements

Kotlin 2.3.20, AGP 8.13.2, Gradle 8.13, JDK 17. The Google Ads 25.4.0 artifact contains Kotlin 2.3 metadata; the Kotlin 2.1 toolchain from 0.1.0 cannot compile it. AGP 8.13.2 provides R8 support for Kotlin 2.3. See https://developer.android.com/build/releases/agp-8-13-0-release-notes.

## Included

- Google Mobile Ads SDK `25.4.0` and UMP `4.0.0`.
- Adaptive banner on the home screen, above navigation; another after a completed session. A visible advertisement label separates the creative from app content.
- No banners on questions, exams, answer review, onboarding, ticket lists, or practice selection. No interstitials, app-open ads, rewarded locks, or ad-dependent navigation.
- UMP consent information refreshes each Activity launch. Required consent forms are loaded first and presented only when the dashboard is visible and resumed. A form that finishes loading during an exam waits for the dashboard.
- Ads initialize once per process, after UMP permits requests. Banner loading additionally waits for SDK initialization. Privacy choices temporarily remove existing banners so subsequent requests reflect the latest consent.
- The privacy-options button appears in home settings and progress settings when UMP requires it. Consent failures offer a retry; learning remains available.
- Banners pause/resume with the lifecycle and destroy on removal or width/orientation changes. An unavailable banner collapses. No application retry loop or forced refresh.

## Development

Debug always uses Google's official sample app and adaptive banner IDs. Production properties do not substitute real IDs into debug inventory. Run a debug build on an emulator or a test device to verify creatives. Never replace the debug IDs with production ad units during testing.

UI/domain automated tests deliberately omit the ad controller and do not request ads through the application controller. They do not prove ad fill, consent geography behaviour, or SDK behaviour on a physical device.

## Production configuration

Create your app and banner ad unit in your own AdMob account. Put the following properties in your user Gradle properties or your CI configuration (substitute actual IDs):

```properties
ADS_ENABLED=true
ADMOB_APP_ID=ca-app-pub-YOUR_PUBLISHER_ID~YOUR_APP_ID
ADMOB_BANNER_ID=ca-app-pub-YOUR_PUBLISHER_ID/YOUR_BANNER_ID
```

Release ads default to disabled when `ADS_ENABLED` is absent. Enabling production ads requires valid IDs belonging to your own account. These IDs are public identifiers, not signing secrets. Release signing remains your own configuration.

Create applicable privacy messages in AdMob Privacy & messaging, add your privacy-policy URL and configure the app's actual audience and distribution settings. This project does not declare the audience child-directed; review that setting against your intended audience before distributing. The app contains INTERNET and ACCESS_NETWORK_STATE; Google SDKs merge additional permissions, including Advertising ID. Review the merged manifest and Play Data safety against your final SDK stack.

Test consent-required and consent-not-required cases with UMP's documented test-device/debug-geography configuration, rotation, background/foreground, consent changes, denied network access, and no-fill responses before release. No production account, ad unit, privacy message, or ad-serving verification has been configured on your behalf.

## Russia availability

Google's published policy pauses AdMob campaigns and AdMob Network serving to users located in Russia. Russia-based publisher monetization is also paused. Third-party waterfall mediation is a separate option under Google's regional policy; no third-party mediation adapters are included here. Do not assume that integrating AdMob provides Google ad revenue from users in Russia.

## References

- SDK setup: https://developers.google.com/admob/android/quick-start
- Adaptive banners and test ID: https://developers.google.com/admob/android/banner
- Consent: https://developers.google.com/admob/android/privacy
- UMP API (deferred form presentation): https://developers.google.com/admob/android/reference/privacy/com/google/android/ump/UserMessagingPlatform
- Regional policy: https://support.google.com/publisherpolicies/answer/15766875?hl=en

Sources checked 2026-09-11. The current setup guide labels the `com.google.android.gms.ads` API as Legacy and offers a Next-Gen migration. This revision uses the documented stable API; a migration should be a separately tested change.
