import Foundation

struct H264NALU {
    static let startCode = Data([0x00, 0x00, 0x00, 0x01])

    let data: Data

    var annexB: Data {
        H264NALU.startCode + data
    }

    var avcc: Data {
        let bigEndianLength = CFSwapInt32HostToBig(UInt32(data.count))
        return withUnsafeBytes(of: bigEndianLength) { Data($0) } + data
    }

    var isPFrame: Bool {
        guard let firstByte = data.first else { return false }
        return (firstByte & 0x1f) == 1
    }

    var isIFrame: Bool {
        guard let firstByte = data.first else { return false }
        return (firstByte & 0x1f) == 5
    }

    var isSPS: Bool {
        guard let firstByte = data.first else { return false }
        return (firstByte & 0x1f) == 7
    }

    var isPPS: Bool {
        guard let firstByte = data.first else { return false }
        return (firstByte & 0x1f) == 8
    }
}
