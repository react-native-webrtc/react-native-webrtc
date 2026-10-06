import VideoToolbox

public extension VideoEncoder {
    struct Config {
        // MARK: Lifecycle

        public init(
            codecType: CMVideoCodecType = kCMVideoCodecType_HEVC,
            maxKeyFrameInterval: Int? = nil,
            maxKeyFrameIntervalDuration: Int? = nil,
            allowTemporalCompression: Bool? = nil,
            allowFrameReordering: Bool? = nil,
            allowOpenGOP: Bool? = nil,
            averageBitRate: Int? = nil,
            dataRateLimits: [Double]? = nil,
            quality: Float? = nil,
            targetQualityForAlpha: Float? = nil,
            moreFramesBeforeStart: Bool? = nil,
            moreFramesAfterEnd: Bool? = nil,
            prioritizeEncodingSpeedOverQuality: Bool? = nil,
            constantBitRate: Int? = nil,
            profileLevel: CFString? = nil,
            outputBitDepth: CFNumber? = nil,
            hdrMetadataInsertionMode: CFString? = nil,
            h264EntropyMode: CFString? = nil,
            depth: CFNumber? = nil,
            preserveAlphaChannel: Bool? = nil,
            maxFrameDelayCount: CFNumber? = nil,
            maxH264SliceBytes: Int32? = nil,
            realTime: Bool? = nil,
            maximizePowerEfficiency: Bool? = nil,
            sourceFrameCount: CFNumber? = nil,
            expectedFrameRate: Int? = nil,
            baseLayerFrameRateFraction: Float? = nil,
            baseLayerBitRateFraction: Float? = nil,
            expectedDuration: CFNumber? = nil,
            baseLayerFrameRate: CFNumber? = nil,
            referenceBufferCount: CFNumber? = nil,
            enableHardwareAcceleratedVideoEncoder: Bool? = nil,
            requireHardwareAcceleratedVideoEncoder: Bool? = nil,
            requiredEncoderGPURegistryID: UInt64? = nil,
            preferredEncoderGPURegistryID: UInt64? = nil,
            cleanAperture: CFDictionary? = nil,
            pixelAspectRatio: CFDictionary? = nil,
            fieldCount: CFNumber? = nil,
            fieldDetail: CFString? = nil,
            aspectRatio16x9: Bool? = nil,
            progressiveScan: Bool? = nil,
            colorPrimaries: CFString? = nil,
            transferFunction: CFString? = nil,
            yCbCrMatrix: CFString? = nil,
            iccProfile: CFData? = nil,
            masteringDisplayColorVolume: CFData? = nil,
            contentLightLevelInfo: CFData? = nil,
            gammaLevel: CFNumber? = nil,
            alphaChannelMode: CFString? = nil,
            pixelTransferProperties: CFDictionary? = nil,
            multiPassStorage: VTMultiPassStorage? = nil,
            encoderID: CFString? = nil,
            preserveDynamicHDRMetadata: Bool? = nil,
            enableLowLatencyRateControl: Bool? = nil,
            maxAllowedFrameQP: CFNumber? = nil,
            minAllowedFrameQP: CFNumber? = nil,
            enableLTR: Bool? = nil,
            mvHEVCVideoLayerIDs: CFArray? = nil,
            mvHEVCViewIDs: CFArray? = nil,
            mvHEVCLeftAndRightViewIDs: CFArray? = nil,
            heroEye: CFString? = nil,
            stereoCameraBaseline: UInt32? = nil,
            horizontalDisparityAdjustment: Int32? = nil,
            hasLeftStereoEyeView: Bool? = nil,
            hasRightStereoEyeView: Bool? = nil
        ) {
            self.codecType = codecType
            self.maxKeyFrameInterval = maxKeyFrameInterval
            self.maxKeyFrameIntervalDuration = maxKeyFrameIntervalDuration
            self.allowTemporalCompression = allowTemporalCompression
            self.allowFrameReordering = allowFrameReordering
            self.allowOpenGOP = allowOpenGOP
            self.averageBitRate = averageBitRate
            self.dataRateLimits = dataRateLimits
            self.quality = quality
            self.targetQualityForAlpha = targetQualityForAlpha
            self.moreFramesBeforeStart = moreFramesBeforeStart
            self.moreFramesAfterEnd = moreFramesAfterEnd
            self.prioritizeEncodingSpeedOverQuality = prioritizeEncodingSpeedOverQuality
            self.constantBitRate = constantBitRate
            self.profileLevel = profileLevel
            self.outputBitDepth = outputBitDepth
            self.hdrMetadataInsertionMode = hdrMetadataInsertionMode
            self.h264EntropyMode = h264EntropyMode
            self.depth = depth
            self.preserveAlphaChannel = preserveAlphaChannel
            self.maxFrameDelayCount = maxFrameDelayCount
            self.maxH264SliceBytes = maxH264SliceBytes
            self.realTime = realTime
            self.maximizePowerEfficiency = maximizePowerEfficiency
            self.sourceFrameCount = sourceFrameCount
            self.expectedFrameRate = expectedFrameRate
            self.baseLayerFrameRateFraction = baseLayerFrameRateFraction
            self.baseLayerBitRateFraction = baseLayerBitRateFraction
            self.expectedDuration = expectedDuration
            self.baseLayerFrameRate = baseLayerFrameRate
            self.referenceBufferCount = referenceBufferCount
            #if os(macOS)
            self.enableHardwareAcceleratedVideoEncoder = enableHardwareAcceleratedVideoEncoder
            self.requireHardwareAcceleratedVideoEncoder = requireHardwareAcceleratedVideoEncoder
            #endif
            self.requiredEncoderGPURegistryID = requiredEncoderGPURegistryID
            self.preferredEncoderGPURegistryID = preferredEncoderGPURegistryID
            self.cleanAperture = cleanAperture
            self.pixelAspectRatio = pixelAspectRatio
            self.fieldCount = fieldCount
            self.fieldDetail = fieldDetail
            self.aspectRatio16x9 = aspectRatio16x9
            self.progressiveScan = progressiveScan
            self.colorPrimaries = colorPrimaries
            self.transferFunction = transferFunction
            self.yCbCrMatrix = yCbCrMatrix
            self.iccProfile = iccProfile
            self.masteringDisplayColorVolume = masteringDisplayColorVolume
            self.contentLightLevelInfo = contentLightLevelInfo
            self.gammaLevel = gammaLevel
            self.alphaChannelMode = alphaChannelMode
            self.pixelTransferProperties = pixelTransferProperties
            self.multiPassStorage = multiPassStorage
            self.encoderID = encoderID
            self.preserveDynamicHDRMetadata = preserveDynamicHDRMetadata
            self.enableLowLatencyRateControl = enableLowLatencyRateControl
            self.maxAllowedFrameQP = maxAllowedFrameQP
            self.minAllowedFrameQP = minAllowedFrameQP
            self.enableLTR = enableLTR
            self.mvHEVCVideoLayerIDs = mvHEVCVideoLayerIDs
            self.mvHEVCViewIDs = mvHEVCViewIDs
            self.mvHEVCLeftAndRightViewIDs = mvHEVCLeftAndRightViewIDs
            self.heroEye = heroEye
            self.stereoCameraBaseline = stereoCameraBaseline
            self.horizontalDisparityAdjustment = horizontalDisparityAdjustment
            self.hasLeftStereoEyeView = hasLeftStereoEyeView
            self.hasRightStereoEyeView = hasRightStereoEyeView
        }

        // MARK: Public

        /// Live capture and live broadcast scenarios.
        /// Also set expectedFrameRate to real-time frame rate if possible
        public static let liveCapture = Config(
            realTime: true
        )

        /// Offline transcode initiated by a user, who is waiting for the results
        public static let activeTranscoding = Config(
            realTime: false,
            maximizePowerEfficiency: false
        )

        /// Offline transcode in the background (when the user is not aware)
        public static let backgroundTranscoding = Config(
            realTime: false,
            maximizePowerEfficiency: false
        )

        /// Ultra-low-latency conferencing and cloud gaming (cases where every millisecond counts).
        /// Also set expectedFrameRate to real-time frame rate if possible
        public static let ultraLowLatency = Config(
            prioritizeEncodingSpeedOverQuality: true,
            realTime: true,
            enableLowLatencyRateControl: true
        )

        public var codecType: CMVideoCodecType

        // MARK: - Frame dependency

        public var maxKeyFrameInterval: Int?
        public var maxKeyFrameIntervalDuration: Int?
        public var allowTemporalCompression: Bool?
        public var allowFrameReordering: Bool?
        public var allowOpenGOP: Bool?

        // MARK: - Rate control

        public var averageBitRate: Int?
        public var dataRateLimits: [Double]?
        public var quality: Float?
        public var targetQualityForAlpha: Float?
        public var moreFramesBeforeStart: Bool?
        public var moreFramesAfterEnd: Bool?
        public var prioritizeEncodingSpeedOverQuality: Bool?
        public var constantBitRate: Int?

        // MARK: - Bitstream configuration

        public var profileLevel: CFString? // TODO: - Enum
        public var outputBitDepth: CFNumber? // TODO: - Better type
        public var hdrMetadataInsertionMode: CFString? // TODO: - Enum
        public var h264EntropyMode: CFString? // TODO: - Enum
        public var depth: CFNumber? // TODO: - Enum
        public var preserveAlphaChannel: Bool?

        // MARK: - Runtime restrictions

        public var maxFrameDelayCount: CFNumber? // TODO: - Better type
        public var maxH264SliceBytes: Int32?
        public var realTime: Bool?
        public var maximizePowerEfficiency: Bool?

        // MARK: - Hints

        public var sourceFrameCount: CFNumber? // TODO: - Better type
        public var expectedFrameRate: Int? // TODO: - Better type
        public var baseLayerFrameRateFraction: Float?
        public var baseLayerBitRateFraction: Float?
        public var expectedDuration: CFNumber? // TODO: - Better type
        public var baseLayerFrameRate: CFNumber? // TODO: - Better type
        public var referenceBufferCount: CFNumber? // TODO: - Better type

        // MARK: - Hardware acceleration

        #if os(macOS)
        public var enableHardwareAcceleratedVideoEncoder: Bool?
        public var requireHardwareAcceleratedVideoEncoder: Bool?
        #endif

        public var requiredEncoderGPURegistryID: UInt64?
        public var preferredEncoderGPURegistryID: UInt64?

        // MARK: - Clean aperture and pixel aspect ratio

        public var cleanAperture: CFDictionary? // TODO: - Better type
        public var pixelAspectRatio: CFDictionary? // TODO: - Better type
        public var fieldCount: CFNumber? // TODO: - Better type
        public var fieldDetail: CFString? // TODO: - Enum
        public var aspectRatio16x9: Bool?
        public var progressiveScan: Bool?

        // MARK: - Color

        public var colorPrimaries: CFString? // TODO: - Enum
        public var transferFunction: CFString? // TODO: - Enum
        public var yCbCrMatrix: CFString? // TODO: - Enum
        public var iccProfile: CFData? // TODO: - Better type
        public var masteringDisplayColorVolume: CFData? // TODO: - Better type
        public var contentLightLevelInfo: CFData? // TODO: - Better type
        public var gammaLevel: CFNumber? // TODO: - Better type
        public var alphaChannelMode: CFString? // TODO: - Enum

        // MARK: - Pre-compression processing

        public var pixelTransferProperties: CFDictionary? // TODO: - Better type

        // MARK: - Multi-pass

        public var multiPassStorage: VTMultiPassStorage?

        // MARK: - Encoder information

        public var encoderID: CFString?
        public var preserveDynamicHDRMetadata: Bool?
        public var enableLowLatencyRateControl: Bool?
        public var maxAllowedFrameQP: CFNumber? // TODO: - Better type
        public var minAllowedFrameQP: CFNumber? // TODO: - Better type
        public var enableLTR: Bool?

        // MARK: - Multi-image compression

        public var mvHEVCVideoLayerIDs: CFArray? // TODO: - Better type
        public var mvHEVCViewIDs: CFArray? // TODO: - Better type
        public var mvHEVCLeftAndRightViewIDs: CFArray? // TODO: - Better type

        // MARK: - VideoExtendedUsage signaling

        public var heroEye: CFString? // TODO: - Enum
        public var stereoCameraBaseline: UInt32?
        public var horizontalDisparityAdjustment: Int32?
        public var hasLeftStereoEyeView: Bool?
        public var hasRightStereoEyeView: Bool?

        // MARK: Internal

        var encoderSpecification: CFDictionary {
            var encoderSpecification: [CFString: CFTypeRef] = [:]
            #if os(macOS)
            if let enableHardwareAcceleratedVideoEncoder {
                encoderSpecification[
                    kVTVideoEncoderSpecification_EnableHardwareAcceleratedVideoEncoder
                ] = enableHardwareAcceleratedVideoEncoder as CFBoolean
            }
            if let requireHardwareAcceleratedVideoEncoder {
                encoderSpecification[
                    kVTVideoEncoderSpecification_RequireHardwareAcceleratedVideoEncoder
                ] = requireHardwareAcceleratedVideoEncoder as CFBoolean
            }
            #endif
            if let requiredEncoderGPURegistryID {
                encoderSpecification[
                    kVTVideoEncoderSpecification_RequiredEncoderGPURegistryID
                ] = requiredEncoderGPURegistryID as CFNumber
            }
            if let preferredEncoderGPURegistryID {
                encoderSpecification[
                    kVTVideoEncoderSpecification_PreferredEncoderGPURegistryID
                ] = preferredEncoderGPURegistryID as CFNumber
            }
            if let enableLowLatencyRateControl {
                encoderSpecification[
                    kVTVideoEncoderSpecification_EnableLowLatencyRateControl
                ] = enableLowLatencyRateControl as CFBoolean
            }
            return encoderSpecification as CFDictionary
        }

        func apply(to compressionSession: VTCompressionSession) {
            if let maxKeyFrameInterval {
                VTSessionSetProperty(
                    compressionSession,
                    key: kVTCompressionPropertyKey_MaxKeyFrameInterval,
                    value: maxKeyFrameInterval as CFNumber
                )
            }
            if let maxKeyFrameIntervalDuration {
                VTSessionSetProperty(
                    compressionSession,
                    key: kVTCompressionPropertyKey_MaxKeyFrameIntervalDuration,
                    value: maxKeyFrameIntervalDuration as CFNumber
                )
            }
            if let allowTemporalCompression {
                VTSessionSetProperty(
                    compressionSession,
                    key: kVTCompressionPropertyKey_AllowTemporalCompression,
                    value: allowTemporalCompression as CFBoolean
                )
            }
            if let allowFrameReordering {
                VTSessionSetProperty(
                    compressionSession,
                    key: kVTCompressionPropertyKey_AllowFrameReordering,
                    value: allowFrameReordering as CFBoolean
                )
            }
            if let allowOpenGOP {
                VTSessionSetProperty(
                    compressionSession,
                    key: kVTCompressionPropertyKey_AllowOpenGOP,
                    value: allowOpenGOP as CFBoolean
                )
            }
            if let averageBitRate {
                VTSessionSetProperty(
                    compressionSession,
                    key: kVTCompressionPropertyKey_AverageBitRate,
                    value: averageBitRate as CFNumber
                )
            }
            if let dataRateLimits {
                VTSessionSetProperty(
                    compressionSession,
                    key: kVTCompressionPropertyKey_DataRateLimits,
                    value: dataRateLimits as CFArray
                )
            }
            if let quality {
                VTSessionSetProperty(
                    compressionSession,
                    key: kVTCompressionPropertyKey_Quality,
                    value: quality as CFNumber
                )
            }
            if let targetQualityForAlpha {
                VTSessionSetProperty(
                    compressionSession,
                    key: kVTCompressionPropertyKey_TargetQualityForAlpha,
                    value: targetQualityForAlpha as CFNumber
                )
            }
            if let moreFramesBeforeStart {
                VTSessionSetProperty(
                    compressionSession,
                    key: kVTCompressionPropertyKey_MoreFramesBeforeStart,
                    value: moreFramesBeforeStart as CFBoolean
                )
            }
            if let moreFramesAfterEnd {
                VTSessionSetProperty(
                    compressionSession,
                    key: kVTCompressionPropertyKey_MoreFramesAfterEnd,
                    value: moreFramesAfterEnd as CFBoolean
                )
            }
            if let prioritizeEncodingSpeedOverQuality {
                VTSessionSetProperty(
                    compressionSession,
                    key: kVTCompressionPropertyKey_PrioritizeEncodingSpeedOverQuality,
                    value: prioritizeEncodingSpeedOverQuality as CFBoolean
                )
            }
            if #available(iOS 16.0, tvOS 16.0, *) {
                if let constantBitRate {
                    VTSessionSetProperty(
                        compressionSession,
                        key: kVTCompressionPropertyKey_ConstantBitRate,
                        value: constantBitRate as CFNumber
                    )
                }
            }
            if let profileLevel {
                VTSessionSetProperty(
                    compressionSession,
                    key: kVTCompressionPropertyKey_ProfileLevel,
                    value: profileLevel as CFString
                )
            }
            if #available(iOS 16.0, tvOS 16.0, *) {
                if let outputBitDepth {
                    VTSessionSetProperty(
                        compressionSession,
                        key: kVTCompressionPropertyKey_OutputBitDepth,
                        value: outputBitDepth as CFNumber
                    )
                }
            }
            if let hdrMetadataInsertionMode {
                VTSessionSetProperty(
                    compressionSession,
                    key: kVTCompressionPropertyKey_HDRMetadataInsertionMode,
                    value: hdrMetadataInsertionMode as CFString
                )
            }
            if let h264EntropyMode {
                VTSessionSetProperty(
                    compressionSession,
                    key: kVTCompressionPropertyKey_H264EntropyMode,
                    value: h264EntropyMode as CFString
                )
            }
            if let depth {
                VTSessionSetProperty(
                    compressionSession,
                    key: kVTCompressionPropertyKey_Depth,
                    value: depth as CFNumber
                )
            }
            if #available(iOS 16.0, tvOS 16.0, *) {
                if let preserveAlphaChannel {
                    VTSessionSetProperty(
                        compressionSession,
                        key: kVTCompressionPropertyKey_PreserveAlphaChannel,
                        value: preserveAlphaChannel as CFBoolean
                    )
                }
            }
            if let maxFrameDelayCount {
                VTSessionSetProperty(
                    compressionSession,
                    key: kVTCompressionPropertyKey_MaxFrameDelayCount,
                    value: maxFrameDelayCount as CFNumber
                )
            }
            if let maxH264SliceBytes {
                VTSessionSetProperty(
                    compressionSession,
                    key: kVTCompressionPropertyKey_MaxH264SliceBytes,
                    value: maxH264SliceBytes as CFNumber
                )
            }
            if let realTime {
                VTSessionSetProperty(
                    compressionSession,
                    key: kVTCompressionPropertyKey_RealTime,
                    value: realTime as CFBoolean
                )
            }
            if let maximizePowerEfficiency {
                VTSessionSetProperty(
                    compressionSession,
                    key: kVTCompressionPropertyKey_MaximizePowerEfficiency,
                    value: maximizePowerEfficiency as CFBoolean
                )
            }
            if let sourceFrameCount {
                VTSessionSetProperty(
                    compressionSession,
                    key: kVTCompressionPropertyKey_SourceFrameCount,
                    value: sourceFrameCount as CFNumber
                )
            }
            if let expectedFrameRate {
                VTSessionSetProperty(
                    compressionSession,
                    key: kVTCompressionPropertyKey_ExpectedFrameRate,
                    value: expectedFrameRate as CFNumber
                )
            }
            if let baseLayerFrameRateFraction {
                VTSessionSetProperty(
                    compressionSession,
                    key: kVTCompressionPropertyKey_BaseLayerFrameRateFraction,
                    value: baseLayerFrameRateFraction as CFNumber
                )
            }
            if let baseLayerBitRateFraction {
                VTSessionSetProperty(
                    compressionSession,
                    key: kVTCompressionPropertyKey_BaseLayerBitRateFraction,
                    value: baseLayerBitRateFraction as CFNumber
                )
            }
            if let expectedDuration {
                VTSessionSetProperty(
                    compressionSession,
                    key: kVTCompressionPropertyKey_ExpectedDuration,
                    value: expectedDuration as CFNumber
                )
            }
            if let baseLayerFrameRate {
                VTSessionSetProperty(
                    compressionSession,
                    key: kVTCompressionPropertyKey_BaseLayerFrameRate,
                    value: baseLayerFrameRate as CFNumber
                )
            }
            if #available(iOS 16.0, tvOS 16.0, *) {
                if let referenceBufferCount {
                    VTSessionSetProperty(
                        compressionSession,
                        key: kVTCompressionPropertyKey_ReferenceBufferCount,
                        value: referenceBufferCount as CFNumber
                    )
                }
            }
            if let cleanAperture {
                VTSessionSetProperty(
                    compressionSession,
                    key: kVTCompressionPropertyKey_CleanAperture,
                    value: cleanAperture as CFDictionary
                )
            }
            if let pixelAspectRatio {
                VTSessionSetProperty(
                    compressionSession,
                    key: kVTCompressionPropertyKey_PixelAspectRatio,
                    value: pixelAspectRatio as CFDictionary
                )
            }
            if let fieldCount {
                VTSessionSetProperty(
                    compressionSession,
                    key: kVTCompressionPropertyKey_FieldCount,
                    value: fieldCount as CFNumber
                )
            }
            if let fieldDetail {
                VTSessionSetProperty(
                    compressionSession,
                    key: kVTCompressionPropertyKey_FieldDetail,
                    value: fieldDetail as CFString
                )
            }
            if let aspectRatio16x9 {
                VTSessionSetProperty(
                    compressionSession,
                    key: kVTCompressionPropertyKey_AspectRatio16x9,
                    value: aspectRatio16x9 as CFBoolean
                )
            }
            if let progressiveScan {
                VTSessionSetProperty(
                    compressionSession,
                    key: kVTCompressionPropertyKey_ProgressiveScan,
                    value: progressiveScan as CFBoolean
                )
            }
            if let colorPrimaries {
                VTSessionSetProperty(
                    compressionSession,
                    key: kVTCompressionPropertyKey_ColorPrimaries,
                    value: colorPrimaries as CFString
                )
            }
            if let transferFunction {
                VTSessionSetProperty(
                    compressionSession,
                    key: kVTCompressionPropertyKey_TransferFunction,
                    value: transferFunction as CFString
                )
            }
            if let yCbCrMatrix {
                VTSessionSetProperty(
                    compressionSession,
                    key: kVTCompressionPropertyKey_YCbCrMatrix,
                    value: yCbCrMatrix as CFString
                )
            }
            if let iccProfile {
                VTSessionSetProperty(
                    compressionSession,
                    key: kVTCompressionPropertyKey_ICCProfile,
                    value: iccProfile as CFData
                )
            }
            if let masteringDisplayColorVolume {
                VTSessionSetProperty(
                    compressionSession,
                    key: kVTCompressionPropertyKey_MasteringDisplayColorVolume,
                    value: masteringDisplayColorVolume as CFData
                )
            }
            if let contentLightLevelInfo {
                VTSessionSetProperty(
                    compressionSession,
                    key: kVTCompressionPropertyKey_ContentLightLevelInfo,
                    value: contentLightLevelInfo as CFData
                )
            }
            if let gammaLevel {
                VTSessionSetProperty(
                    compressionSession,
                    key: kVTCompressionPropertyKey_GammaLevel,
                    value: gammaLevel as CFNumber
                )
            }
            if let alphaChannelMode {
                VTSessionSetProperty(
                    compressionSession,
                    key: kVTCompressionPropertyKey_AlphaChannelMode,
                    value: alphaChannelMode as CFString
                )
            }
            if let pixelTransferProperties {
                VTSessionSetProperty(
                    compressionSession,
                    key: kVTCompressionPropertyKey_PixelTransferProperties,
                    value: pixelTransferProperties as CFDictionary
                )
            }
            if let multiPassStorage {
                VTSessionSetProperty(
                    compressionSession,
                    key: kVTCompressionPropertyKey_MultiPassStorage,
                    value: multiPassStorage
                )
            }
            if let encoderID {
                VTSessionSetProperty(
                    compressionSession,
                    key: kVTCompressionPropertyKey_EncoderID,
                    value: encoderID as CFString
                )
            }
            if let preserveDynamicHDRMetadata {
                VTSessionSetProperty(
                    compressionSession,
                    key: kVTCompressionPropertyKey_PreserveDynamicHDRMetadata,
                    value: preserveDynamicHDRMetadata as CFBoolean
                )
            }
            if let maxAllowedFrameQP {
                VTSessionSetProperty(
                    compressionSession,
                    key: kVTCompressionPropertyKey_MaxAllowedFrameQP,
                    value: maxAllowedFrameQP as CFNumber
                )
            }
            if #available(iOS 16.0, tvOS 16.0, *) {
                if let minAllowedFrameQP {
                    VTSessionSetProperty(
                        compressionSession,
                        key: kVTCompressionPropertyKey_MinAllowedFrameQP,
                        value: minAllowedFrameQP as CFNumber
                    )
                }
            }
            if let enableLTR {
                VTSessionSetProperty(
                    compressionSession,
                    key: kVTCompressionPropertyKey_EnableLTR,
                    value: enableLTR as CFBoolean
                )
            }
            #if os(macOS) || os(iOS) || os(visionOS)
            if #available(macOS 14.0, iOS 17.0, *) {
                if let mvHEVCVideoLayerIDs {
                    VTSessionSetProperty(
                        compressionSession,
                        key: kVTCompressionPropertyKey_MVHEVCVideoLayerIDs,
                        value: mvHEVCVideoLayerIDs as CFArray
                    )
                }
                if let mvHEVCViewIDs {
                    VTSessionSetProperty(
                        compressionSession,
                        key: kVTCompressionPropertyKey_MVHEVCViewIDs,
                        value: mvHEVCViewIDs as CFArray
                    )
                }
                if let mvHEVCLeftAndRightViewIDs {
                    VTSessionSetProperty(
                        compressionSession,
                        key: kVTCompressionPropertyKey_MVHEVCLeftAndRightViewIDs,
                        value: mvHEVCLeftAndRightViewIDs as CFArray
                    )
                }
                if let heroEye {
                    VTSessionSetProperty(
                        compressionSession,
                        key: kVTCompressionPropertyKey_HeroEye,
                        value: heroEye as CFString
                    )
                }
                if let stereoCameraBaseline {
                    VTSessionSetProperty(
                        compressionSession,
                        key: kVTCompressionPropertyKey_StereoCameraBaseline,
                        value: stereoCameraBaseline as CFNumber
                    )
                }
                if let horizontalDisparityAdjustment {
                    VTSessionSetProperty(
                        compressionSession,
                        key: kVTCompressionPropertyKey_HorizontalDisparityAdjustment,
                        value: horizontalDisparityAdjustment as CFNumber
                    )
                }
                if let hasLeftStereoEyeView {
                    VTSessionSetProperty(
                        compressionSession,
                        key: kVTCompressionPropertyKey_HasLeftStereoEyeView,
                        value: hasLeftStereoEyeView as CFBoolean
                    )
                }
                if let hasRightStereoEyeView {
                    VTSessionSetProperty(
                        compressionSession,
                        key: kVTCompressionPropertyKey_HasRightStereoEyeView,
                        value: hasRightStereoEyeView as CFBoolean
                    )
                }
            }
            #endif
        }
    }
}
