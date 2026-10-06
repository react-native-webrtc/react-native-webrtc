import Foundation

func getLastError(prefix: String, fallbackCode: Int32 = -1) -> NSError {
    let errCode = errno
    let message = String(cString: strerror(errCode))
    let code: Int32 = (errCode != 0) ? errCode : fallbackCode
    return NSError(domain: prefix, code: Int(code), userInfo: [
        NSLocalizedDescriptionKey: "\(prefix): \(message)"
    ])
}

final class UnixSocketClient {
    private let socketPath: String
    private var socketFD: Int32 = -1

    init(socketPath: String) {
        self.socketPath = socketPath
    }

    func connect() throws {
        socketFD = socket(AF_UNIX, SOCK_STREAM, 0)
        guard socketFD >= 0 else { throw getLastError(prefix: "Socket creation failed") }

        var addr = sockaddr_un()
        addr.sun_family = sa_family_t(AF_UNIX)
        _ = withUnsafeMutablePointer(to: &addr.sun_path) { ptr in
            socketPath.withCString {
              strncpy(ptr, $0, socketPath.count)
            }
        }

        let len = socklen_t(MemoryLayout<sockaddr_un>.stride)
        let connectResult = withUnsafePointer(to: &addr) {
            $0.withMemoryRebound(to: sockaddr.self, capacity: 1) {
                Darwin.connect(socketFD, $0, len)
            }
        }
        guard connectResult == 0 else {
            throw getLastError(prefix: "Connect failed")
        }
        
        var noSigPipe: Int32 = 1
        setsockopt(socketFD, SOL_SOCKET, SO_NOSIGPIPE, &noSigPipe, socklen_t(MemoryLayout.size(ofValue: noSigPipe)))
    }

    func send(data: UnsafeRawPointer, length: Int) throws {
        var buffer = Data()
        var lengthPrefix = UInt32(length).bigEndian
        buffer.append(Data(bytes: &lengthPrefix, count: MemoryLayout<UInt32>.size))
        buffer.append(Data(bytes: data, count: length))

        let result = buffer.withUnsafeBytes {
            write(socketFD, $0.baseAddress, $0.count)
        }

        if result < 0 {
            throw getLastError(prefix: "Write failed")
        }
    }

    func close() {
        if socketFD >= 0 {
            Darwin.close(socketFD)
        }
    }
}
