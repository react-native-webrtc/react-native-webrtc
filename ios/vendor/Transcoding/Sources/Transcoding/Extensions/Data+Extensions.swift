import Foundation

extension Data {
    func split(separator: Data) -> [Data] {
        var chunks: [Data] = []
        var position = startIndex
        while let range = self[position...].range(of: separator) {
            if range.lowerBound > position {
                chunks.append(self[position..<range.lowerBound])
            }
            position = range.upperBound
        }
        if position < endIndex {
            chunks.append(self[position..<endIndex])
        }
        return chunks
    }
}
