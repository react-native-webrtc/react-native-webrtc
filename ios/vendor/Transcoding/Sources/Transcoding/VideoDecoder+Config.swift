import VideoToolbox

public extension VideoDecoder {
    struct Config {
        // MARK: Lifecycle

        public init(
            imageBufferAttributes: CFDictionary? = nil,
            outputPoolRequestedMinimumBufferCount: Int? = nil,
            enableHardwareAcceleratedVideoDecoder: Bool? = nil,
            requireHardwareAcceleratedVideoDecoder: Bool? = nil,
            realTime: Bool? = nil,
            maximizePowerEfficiency: Bool? = nil,
            threadCount: Int? = nil,
            fieldMode: CFString? = nil,
            deinterlaceMode: CFString? = nil,
            reducedResolutionDecode: CGSize? = nil,
            reducedCoefficientDecode: CFNumber? = nil,
            reducedFrameDelivery: Float? = nil,
            onlyTheseFrames: CFString? = nil,
            temporalLevelLimit: CFNumber? = nil,
            suggestedQualityOfServiceTiers: CFArray? = nil,
            pixelTransferProperties: CFDictionary? = nil,
            requiredDecoderGPURegistryID: UInt32? = nil,
            preferredDecoderGPURegistryID: UInt32? = nil,
            propagatePerFrameHDRDisplayMetadata: Bool? = nil,
            generatePerFrameHDRDisplayMetadata: Bool? = nil,
            requestedMVHEVCVideoLayerIDs: [Int64]? = nil
        ) {
            self.imageBufferAttributes = imageBufferAttributes
            self.outputPoolRequestedMinimumBufferCount = outputPoolRequestedMinimumBufferCount
            self.enableHardwareAcceleratedVideoDecoder = enableHardwareAcceleratedVideoDecoder
            self.requireHardwareAcceleratedVideoDecoder = requireHardwareAcceleratedVideoDecoder
            self.realTime = realTime
            self.maximizePowerEfficiency = maximizePowerEfficiency
            self.threadCount = threadCount
            self.fieldMode = fieldMode
            self.deinterlaceMode = deinterlaceMode
            self.reducedResolutionDecode = reducedResolutionDecode
            self.reducedCoefficientDecode = reducedCoefficientDecode
            self.reducedFrameDelivery = reducedFrameDelivery
            self.onlyTheseFrames = onlyTheseFrames
            self.temporalLevelLimit = temporalLevelLimit
            self.suggestedQualityOfServiceTiers = suggestedQualityOfServiceTiers
            self.pixelTransferProperties = pixelTransferProperties
            self.requiredDecoderGPURegistryID = requiredDecoderGPURegistryID
            self.preferredDecoderGPURegistryID = preferredDecoderGPURegistryID
            self.propagatePerFrameHDRDisplayMetadata = propagatePerFrameHDRDisplayMetadata
            self.generatePerFrameHDRDisplayMetadata = generatePerFrameHDRDisplayMetadata
            self.requestedMVHEVCVideoLayerIDs = requestedMVHEVCVideoLayerIDs
        }

        // MARK: Public

        // MARK: - Image buffer attributes

        public var imageBufferAttributes: CFDictionary?

        // MARK: - Pixel buffer pools

        public var outputPoolRequestedMinimumBufferCount: Int?

        // MARK: - Hardware acceleration

        public var enableHardwareAcceleratedVideoDecoder: Bool?
        public var requireHardwareAcceleratedVideoDecoder: Bool?

        // MARK: - Decoder behavior

        public var realTime: Bool?
        public var maximizePowerEfficiency: Bool?
        public var threadCount: Int?
        public var fieldMode: CFString? // TODO: - Better type
        public var deinterlaceMode: CFString? // TODO: - Better type
        public var reducedResolutionDecode: CGSize?
        public var reducedCoefficientDecode: CFNumber?
        public var reducedFrameDelivery: Float?
        public var onlyTheseFrames: CFString? // TODO: - Enum?
        public var temporalLevelLimit: CFNumber? // TODO: - Better type
        public var suggestedQualityOfServiceTiers: CFArray? // TODO: - Better type?

        // MARK: - Post-decompression processing

        public var pixelTransferProperties: CFDictionary? // TODO: - Better type
        public var requiredDecoderGPURegistryID: UInt32?
        public var preferredDecoderGPURegistryID: UInt32?
        public var propagatePerFrameHDRDisplayMetadata: Bool?
        public var generatePerFrameHDRDisplayMetadata: Bool?

        // MARK: - Multi-image decompression

        public var requestedMVHEVCVideoLayerIDs: [Int64]?

        // MARK: Internal

        var decoderSpecification: CFDictionary {
            var decoderSpecification: [CFString: CFTypeRef] = [:]
            if #available(iOS 17.0, tvOS 17.0, *) {
                if let enableHardwareAcceleratedVideoDecoder {
                    decoderSpecification[
                        kVTVideoDecoderSpecification_EnableHardwareAcceleratedVideoDecoder
                    ] = enableHardwareAcceleratedVideoDecoder as CFBoolean
                }
                if let requireHardwareAcceleratedVideoDecoder {
                    decoderSpecification[
                        kVTVideoDecoderSpecification_RequireHardwareAcceleratedVideoDecoder
                    ] = requireHardwareAcceleratedVideoDecoder as CFBoolean
                }
            }
            if let requiredDecoderGPURegistryID {
                decoderSpecification[
                    kVTVideoDecoderSpecification_RequiredDecoderGPURegistryID
                ] = requiredDecoderGPURegistryID as CFNumber
            }
            if let preferredDecoderGPURegistryID {
                decoderSpecification[
                    kVTVideoDecoderSpecification_PreferredDecoderGPURegistryID
                ] = preferredDecoderGPURegistryID as CFNumber
            }
            return decoderSpecification as CFDictionary
        }

        func apply(to decompressionSession: VTDecompressionSession) {
            if let outputPoolRequestedMinimumBufferCount {
                VTSessionSetProperty(
                    decompressionSession,
                    key: kVTDecompressionPropertyKey_OutputPoolRequestedMinimumBufferCount,
                    value: outputPoolRequestedMinimumBufferCount as CFNumber
                )
            }
            if let realTime {
                VTSessionSetProperty(
                    decompressionSession,
                    key: kVTDecompressionPropertyKey_RealTime,
                    value: realTime as CFBoolean
                )
            }
            if let maximizePowerEfficiency {
                VTSessionSetProperty(
                    decompressionSession,
                    key: kVTDecompressionPropertyKey_MaximizePowerEfficiency,
                    value: maximizePowerEfficiency as CFBoolean
                )
            }
            if let threadCount {
                VTSessionSetProperty(
                    decompressionSession,
                    key: kVTDecompressionPropertyKey_ThreadCount,
                    value: threadCount as CFNumber
                )
            }
            if let fieldMode {
                VTSessionSetProperty(
                    decompressionSession,
                    key: kVTDecompressionPropertyKey_FieldMode,
                    value: fieldMode as CFString
                )
            }
            if let deinterlaceMode {
                VTSessionSetProperty(
                    decompressionSession,
                    key: kVTDecompressionPropertyKey_DeinterlaceMode,
                    value: deinterlaceMode as CFString
                )
            }
            if let reducedResolutionDecode {
                VTSessionSetProperty(
                    decompressionSession,
                    key: kVTDecompressionPropertyKey_ReducedResolutionDecode,
                    value: [
                        kVTDecompressionResolutionKey_Width: reducedResolutionDecode.width as CFNumber,
                        kVTDecompressionResolutionKey_Height: reducedResolutionDecode.height as CFNumber,
                    ] as CFDictionary
                )
            }
            if let reducedCoefficientDecode {
                VTSessionSetProperty(
                    decompressionSession,
                    key: kVTDecompressionPropertyKey_ReducedCoefficientDecode,
                    value: reducedCoefficientDecode as CFNumber
                )
            }
            if let reducedFrameDelivery {
                VTSessionSetProperty(
                    decompressionSession,
                    key: kVTDecompressionPropertyKey_ReducedFrameDelivery,
                    value: reducedFrameDelivery as CFNumber
                )
            }
            if let onlyTheseFrames {
                VTSessionSetProperty(
                    decompressionSession,
                    key: kVTDecompressionPropertyKey_OnlyTheseFrames,
                    value: onlyTheseFrames as CFString
                )
            }
            if let temporalLevelLimit {
                VTSessionSetProperty(
                    decompressionSession,
                    key: kVTDecompressionProperty_TemporalLevelLimit,
                    value: temporalLevelLimit as CFNumber
                )
            }
            if let suggestedQualityOfServiceTiers {
                VTSessionSetProperty(
                    decompressionSession,
                    key: kVTDecompressionPropertyKey_SuggestedQualityOfServiceTiers,
                    value: suggestedQualityOfServiceTiers as CFArray
                )
            }
            if let pixelTransferProperties {
                VTSessionSetProperty(
                    decompressionSession,
                    key: kVTDecompressionPropertyKey_PixelTransferProperties,
                    value: pixelTransferProperties as CFDictionary
                )
            }
            if let propagatePerFrameHDRDisplayMetadata {
                VTSessionSetProperty(
                    decompressionSession,
                    key: kVTDecompressionPropertyKey_PropagatePerFrameHDRDisplayMetadata,
                    value: propagatePerFrameHDRDisplayMetadata as CFBoolean
                )
            }
            if #available(macOS 14.0, iOS 17.0, tvOS 17.0, *) {
                if let generatePerFrameHDRDisplayMetadata {
                    VTSessionSetProperty(
                        decompressionSession,
                        key: kVTDecompressionPropertyKey_GeneratePerFrameHDRDisplayMetadata,
                        value: generatePerFrameHDRDisplayMetadata as CFBoolean
                    )
                }
                #if os(iOS) || os(macOS) || os(visionOS)
                if let requestedMVHEVCVideoLayerIDs {
                    VTSessionSetProperty(
                        decompressionSession,
                        key: kVTDecompressionPropertyKey_RequestedMVHEVCVideoLayerIDs,
                        value: requestedMVHEVCVideoLayerIDs as CFArray
                    )
                }
                #endif
            }
        }
    }
}
