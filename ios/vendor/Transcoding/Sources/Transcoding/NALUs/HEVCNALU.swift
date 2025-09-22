import Foundation

struct HEVCNALU {
    static let startCode = Data([0x00, 0x00, 0x00, 0x01])

    let data: Data

    var annexB: Data {
        HEVCNALU.startCode + data
    }

    var avcc: Data {
        let bigEndianLength = CFSwapInt32HostToBig(UInt32(data.count))
        return withUnsafeBytes(of: bigEndianLength) { Data($0) } + data
    }

    var isPFrame: Bool {
        guard let firstByte = data.first else { return false }
        return ((firstByte & 0x7e) >> 1) == 1
    }

    var isIFrame: Bool {
        guard let firstByte = data.first else { return false }
        return ((firstByte & 0x7e) >> 1) == 20
    }

    var isVPS: Bool {
        guard let firstByte = data.first else { return false }
        return ((firstByte & 0x7e) >> 1) == 32
    }

    var isSPS: Bool {
        guard let firstByte = data.first else { return false }
        return ((firstByte & 0x7e) >> 1) == 33
    }

    var isPPS: Bool {
        guard let firstByte = data.first else { return false }
        return ((firstByte & 0x7e) >> 1) == 34
    }
}
