from pathlib import Path
from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "tmp" / "logo-transparent.png"
ASSETS = ROOT / "src" / "main" / "resources" / "static" / "assets"


def contain(image: Image.Image, size: int, padding: int) -> Image.Image:
    canvas = Image.new("RGBA", (size, size), (255, 255, 255, 0))
    image = image.copy()
    image.thumbnail((size - padding * 2, size - padding * 2), Image.Resampling.LANCZOS)
    canvas.alpha_composite(image, ((size - image.width) // 2, (size - image.height) // 2))
    return canvas


def main() -> None:
    ASSETS.mkdir(parents=True, exist_ok=True)
    logo = Image.open(SOURCE).convert("RGBA")
    alpha = logo.getchannel("A")
    bounds = alpha.getbbox()
    if not bounds:
        raise RuntimeError("A logo processada não contém pixels visíveis.")

    cropped = logo.crop(bounds)
    cropped.save(ASSETS / "stowup-logo.png", optimize=True)

    # O símbolo ocupa a região superior central da arte fornecida.
    icon_area = logo.crop((560, 45, 1215, 545))
    icon_bounds = icon_area.getchannel("A").getbbox()
    if not icon_bounds:
        raise RuntimeError("Não foi possível localizar o símbolo da marca.")
    icon = icon_area.crop(icon_bounds)
    contain(icon, 192, 18).save(ASSETS / "icon-192.png", optimize=True)
    contain(icon, 512, 42).save(ASSETS / "icon-512.png", optimize=True)
    contain(icon, 180, 16).save(ASSETS / "apple-touch-icon.png", optimize=True)

    print(f"Logo: {cropped.size}, alpha={cropped.getchannel('A').getextrema()}")
    print("Ícones PWA: 192, 512 e 180 px")


if __name__ == "__main__":
    main()
