import Foundation
import VideoToolbox

public enum VideoTranscoderError: Error {
    case propertyNotSupported
    case propertyReadOnly
    case parameter
    case invalidSession
    case allocationFailed
    case pixelTransferNotSupported
    case couldNotFindVideoDecoder
    case couldNotCreateInstance
    case couldNotFindVideoEncoder
    case videoDecoderBadData
    case videoDecoderUnsupportedDataFormat
    case videoDecoderMalfunction
    case videoEncoderMalfunction
    case videoDecoderNotAvailableNow
    case imageRotationNotSupported
    case pixelRotationNotSupported
    case videoEncoderNotAvailableNow
    case formatDescriptionChangeNotSupported
    case insufficientSourceColorData
    case couldNotCreateColorCorrectionData
    case colorSyncTransformConvertFailed
    case videoDecoderAuthorization
    case videoEncoderAuthorization
    case colorCorrectionPixelTransferFailed
    case multiPassStorageIdentifierMismatch
    case multiPassStorageInvalid
    case frameSiloInvalidTimeStamp
    case frameSiloInvalidTimeRange
    case couldNotFindTemporalFilter
    case pixelTransferNotPermitted
    case colorCorrectionImageRotationFailed
    case videoDecoderRemoved
    case sessionMalfunction
    case videoDecoderNeedsRosetta
    case videoEncoderNeedsRosetta
    case videoDecoderReferenceMissing
    case videoDecoderCallbackMessaging
    case videoDecoderUnknown
    case extensionDisabled
    case videoEncoderMVHEVCVideoLayerIDsMismatch
    case couldNotOutputTaggedBufferGroup
    case unknown(OSStatus)

    // MARK: Lifecycle

    init?(status: OSStatus) {
        guard status != noErr else { return nil }
        switch status {
        case kVTPropertyNotSupportedErr:
            self = .propertyNotSupported
        case kVTPropertyReadOnlyErr:
            self = .propertyReadOnly
        case kVTParameterErr:
            self = .parameter
        case kVTInvalidSessionErr:
            self = .invalidSession
        case kVTAllocationFailedErr:
            self = .allocationFailed
        case kVTPixelTransferNotSupportedErr:
            self = .pixelTransferNotPermitted
        case kVTCouldNotFindVideoDecoderErr:
            self = .couldNotFindVideoDecoder
        case kVTCouldNotCreateInstanceErr:
            self = .couldNotCreateInstance
        case kVTCouldNotFindVideoEncoderErr:
            self = .couldNotFindVideoEncoder
        case kVTVideoDecoderBadDataErr:
            self = .videoDecoderBadData
        case kVTVideoDecoderUnsupportedDataFormatErr:
            self = .videoDecoderUnsupportedDataFormat
        case kVTVideoDecoderMalfunctionErr:
            self = .videoDecoderMalfunction
        case kVTVideoEncoderMalfunctionErr:
            self = .videoEncoderMalfunction
        case kVTVideoDecoderNotAvailableNowErr:
            self = .videoDecoderNotAvailableNow
        case kVTImageRotationNotSupportedErr:
            self = .imageRotationNotSupported
        case kVTPixelRotationNotSupportedErr:
            self = .pixelRotationNotSupported
        case kVTVideoEncoderNotAvailableNowErr:
            self = .videoEncoderNotAvailableNow
        case kVTFormatDescriptionChangeNotSupportedErr:
            self = .formatDescriptionChangeNotSupported
        case kVTInsufficientSourceColorDataErr:
            self = .insufficientSourceColorData
        case kVTCouldNotCreateColorCorrectionDataErr:
            self = .couldNotCreateColorCorrectionData
        case kVTColorSyncTransformConvertFailedErr:
            self = .colorSyncTransformConvertFailed
        case kVTVideoDecoderAuthorizationErr:
            self = .videoDecoderAuthorization
        case kVTVideoEncoderAuthorizationErr:
            self = .videoEncoderAuthorization
        case kVTColorCorrectionPixelTransferFailedErr:
            self = .colorCorrectionPixelTransferFailed
        case kVTMultiPassStorageIdentifierMismatchErr:
            self = .multiPassStorageIdentifierMismatch
        case kVTMultiPassStorageInvalidErr:
            self = .multiPassStorageInvalid
        case kVTFrameSiloInvalidTimeStampErr:
            self = .frameSiloInvalidTimeStamp
        case kVTFrameSiloInvalidTimeRangeErr:
            self = .frameSiloInvalidTimeRange
        case kVTCouldNotFindTemporalFilterErr:
            self = .couldNotFindTemporalFilter
        case kVTPixelTransferNotPermittedErr:
            self = .pixelTransferNotPermitted
        case kVTColorCorrectionImageRotationFailedErr:
            self = .colorCorrectionImageRotationFailed
        case kVTVideoDecoderRemovedErr:
            self = .videoDecoderRemoved
        case kVTSessionMalfunctionErr:
            self = .sessionMalfunction
        case kVTVideoDecoderNeedsRosettaErr:
            self = .videoDecoderNeedsRosetta
        case kVTVideoEncoderNeedsRosettaErr:
            self = .videoEncoderNeedsRosetta
        case kVTVideoDecoderReferenceMissingErr:
            self = .videoDecoderReferenceMissing
        case kVTVideoDecoderCallbackMessagingErr:
            self = .videoDecoderCallbackMessaging
        case kVTVideoDecoderUnknownErr:
            self = .videoDecoderUnknown
        case kVTExtensionDisabledErr:
            self = .extensionDisabled
        case kVTVideoEncoderMVHEVCVideoLayerIDsMismatchErr:
            self = .videoEncoderMVHEVCVideoLayerIDsMismatch
        case kVTCouldNotOutputTaggedBufferGroupErr:
            self = .couldNotOutputTaggedBufferGroup
        default:
            self = .unknown(status)
        }
    }
}
