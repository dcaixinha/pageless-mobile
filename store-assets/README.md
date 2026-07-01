# Store assets

Source material for the F-Droid / Play Store listing (screenshots, framed
exports, and listing copy). Only **finished, framed exports** ever go into the
Fastlane metadata tree
(`fastlane/metadata/android/en-US/images/phoneScreenshots/`); everything here is
working material.

## Directory layout

```
store-assets/
  screenshots/
    raw/        # clean device screenshots, ready to feed into Snapframe
    snapframe/  # framed exports produced by Snapframe (optional working copies)
```

`raw/` holds the debugging-icon-free captures used as Snapframe input. The
untouched originals straight off the device are kept **outside this repo** (they
are large and only needed once).

## Capturing screenshots

Take clean captures on a physical device (see the root `README.md` /
`AGENTS.md` for device setup):

- Use System UI Demo Mode or otherwise get a clean status bar (fixed clock,
  full battery/signal, no notifications).
- Hide personal data: no real server host, email, or tokens. For the Account
  screen, temporarily hard-code a display value like
  `https://pageless.example.com` (`example.com` is reserved for docs), capture,
  then revert.
- Use the same public-domain sample library across every shot for a consistent,
  premium look.

## Removing the wireless-debugging icon

Wireless/USB debugging leaves a small swirl icon in the status bar that Demo
Mode does not always suppress. Because the status-bar background around it is a
single flat color, the fastest fix is to paint a rectangle over the icon using
each image's own local background color. This is lossless for the surrounding
pixels and needs no image editor.

The icon sits at the same spot on every capture from this device (Pixel,
1080×2410): bounding box roughly `x 208–241`, `y 71–104`. Paint a padded box of
`(198, 60)–(252, 116)` to cover antialiasing.

Run this from `store-assets/screenshots/` (requires Python + Pillow:
`pip install Pillow`). It reads the originals and writes icon-free copies into
`raw/`:

```python
from PIL import Image
import glob, os

# Point this at wherever the untouched originals live (outside the repo).
ORIGINALS = os.path.expanduser("~/pageless-screenshots-originals")
box = (198, 60, 252, 116)  # padded bounding box over the swirl icon

os.makedirs("raw", exist_ok=True)
for src in sorted(glob.glob(os.path.join(ORIGINALS, "*.png"))):
    im = Image.open(src).convert("RGB")
    # Sample the flat background just left of the icon gap; fall back to a
    # clearly-empty spot if that pixel isn't part of the background.
    bg = im.getpixel((195, 88))
    if im.getpixel((256, 88)) != bg:
        bg = im.getpixel((450, 88))
    px = im.load()
    for y in range(box[1], box[3]):
        for x in range(box[0], box[2]):
            px[x, y] = bg
    dst = os.path.join("raw", os.path.basename(src))
    im.save(dst)
    print(f"{os.path.basename(src)} painted with {bg}")
```

Verify the region is uniform afterwards (should print `0`):

```python
from PIL import Image
im = Image.open("raw/01_home_dark.png").convert("RGB")
bg = im.getpixel((195, 88))
d = lambda p: sum(abs(a - b) for a, b in zip(p, bg))
print(max(d(im.getpixel((x, y))) for y in range(60, 116) for x in range(198, 252)))
```

If the device or resolution changes, re-derive the bounding box: crop the top of
one screenshot, locate the swirl, and expand a few pixels for antialiasing.

## Framing (Snapframe)

Feed the `raw/` images into Snapframe (<https://pawandeep-prog.github.io/Snapframe/>):

- Choose an **Android/Pixel** frame that matches the capture aspect ratio
  (1080×2410 is 20:9), not the default iPhone frame.
- Use the Pageless purple accent (`#8B5CF6`); keep headlines short (sentence
  case) and away from important UI.
- Export the framed slides, then copy the final images into
  `fastlane/metadata/android/en-US/images/phoneScreenshots/` with ordered
  filenames (`01_…`, `02_…`) — alphabetical order controls listing order.

See `fastlane/metadata/android/en-US/` for the current listing copy and the
screenshot storyboard.
