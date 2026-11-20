import AppKit
import Foundation

func fail(_ message: String) -> Never {
    fputs("YORY icon generation failed: \(message)\n", stderr)
    exit(1)
}

guard CommandLine.arguments.count == 3 else {
    fail("usage: swift tools/generate_ios_app_icon.swift <source-logo.jpeg> <output.png>")
}

let inputURL = URL(fileURLWithPath: CommandLine.arguments[1])
let outputURL = URL(fileURLWithPath: CommandLine.arguments[2])
guard let source = NSImage(contentsOf: inputURL) else {
    fail("could not read the canonical LOGO7.jpeg source")
}

var proposedRect = CGRect(origin: .zero, size: source.size)
guard let sourceCGImage = source.cgImage(forProposedRect: &proposedRect, context: nil, hints: nil) else {
    fail("could not decode the canonical logo image")
}

let canvasSize = 1024
guard let bitmap = NSBitmapImageRep(
    bitmapDataPlanes: nil,
    pixelsWide: canvasSize,
    pixelsHigh: canvasSize,
    bitsPerSample: 8,
    samplesPerPixel: 4,
    hasAlpha: true,
    isPlanar: false,
    colorSpaceName: .deviceRGB,
    bytesPerRow: 0,
    bitsPerPixel: 0
), let graphics = NSGraphicsContext(bitmapImageRep: bitmap) else {
    fail("could not create the 1024x1024 icon canvas")
}

NSGraphicsContext.saveGraphicsState()
NSGraphicsContext.current = graphics
graphics.imageInterpolation = .high

let background = NSColor(calibratedRed: 0.035, green: 0.055, blue: 0.095, alpha: 1)
background.setFill()
NSBezierPath(rect: CGRect(x: 0, y: 0, width: canvasSize, height: canvasSize)).fill()

let safeBox: CGFloat = 760
let ratio = min(safeBox / CGFloat(sourceCGImage.width), safeBox / CGFloat(sourceCGImage.height))
let width = CGFloat(sourceCGImage.width) * ratio
let height = CGFloat(sourceCGImage.height) * ratio
let destination = CGRect(
    x: (CGFloat(canvasSize) - width) / 2,
    y: (CGFloat(canvasSize) - height) / 2,
    width: width,
    height: height
)
graphics.cgContext.interpolationQuality = .high
graphics.cgContext.draw(sourceCGImage, in: destination)
graphics.flushGraphics()
NSGraphicsContext.restoreGraphicsState()

guard let png = bitmap.representation(using: .png, properties: [:]) else {
    fail("could not encode the app icon as PNG")
}
do {
    try FileManager.default.createDirectory(
        at: outputURL.deletingLastPathComponent(),
        withIntermediateDirectories: true
    )
    try png.write(to: outputURL, options: .atomic)
    print("Generated a 1024x1024 YORY KEYS App Store icon from LOGO7.jpeg.")
} catch {
    fail(error.localizedDescription)
}
