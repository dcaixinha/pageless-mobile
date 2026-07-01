# Store images (Fastlane / F-Droid layout)

Add store assets here; F-Droid and Play both read this structure:

    icon.png                    # 512x512 launcher icon (optional; F-Droid can
                                # also extract it from the app)
    featureGraphic.png          # 1024x500 feature graphic (optional)
    phoneScreenshots/           # 1.png, 2.png, ... (24-bit PNG/JPG; recommended)

Screenshots are strongly recommended for a good F-Droid listing but are not
required for the build to succeed. Google Play does not accept an alpha channel
in screenshots; export or convert them to opaque RGB before publishing.
