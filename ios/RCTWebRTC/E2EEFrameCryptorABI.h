// Minimal ABI-compatible declarations of the WebRTC M124 C++ interfaces used
// by the E2EE frame cryptor. The Jitsi WebRTC binaries ship no C++ headers
// and hide C++ symbols, so these are vendored copies of the exact upstream
// declarations (api/ref_count.h, api/scoped_refptr.h, api/array_view.h,
// api/media_types.h, api/crypto/frame_encryptor_interface.h,
// api/crypto/frame_decryptor_interface.h), reduced to what is needed here.
//
// Layouts and vtable orders must match the prebuilt binary exactly: do not
// reorder members or virtual functions, do not add virtual functions, and do
// not change any type used in a signature.
//
// This file is shared verbatim between Android (android/src/main/cpp) and
// iOS (ios/RCTWebRTC).

#ifndef E2EE_FRAME_CRYPTOR_ABI_H_
#define E2EE_FRAME_CRYPTOR_ABI_H_

#include <cstddef>
#include <cstdint>
#include <vector>

namespace rtc {

// rtc::ArrayView<T> has the layout { T* data_; size_t size_; }, is trivially
// copyable and is passed by value.
template <typename T>
class ArrayView {
   public:
    ArrayView() : data_(nullptr), size_(0) {}
    ArrayView(T *data, size_t size) : data_(data), size_(size) {}

    T *data() const { return data_; }
    size_t size() const { return size_; }
    bool empty() const { return size_ == 0; }
    T &operator[](size_t index) const { return data_[index]; }

   private:
    T *data_;
    size_t size_;
};

}  // namespace rtc

namespace webrtc {

enum class RefCountReleaseStatus { kDroppedLastRef, kOtherRefsRemained };

class RefCountInterface {
   public:
    virtual void AddRef() const = 0;
    virtual RefCountReleaseStatus Release() const = 0;

   protected:
    virtual ~RefCountInterface() {}
};

// rtc::scoped_refptr<T> has the layout { T* ptr_; }. The (copy) constructor
// calls ptr_->AddRef() and the destructor calls ptr_->Release().
template <class T>
class scoped_refptr {
   public:
    scoped_refptr() : ptr_(nullptr) {}
    explicit scoped_refptr(T *p) : ptr_(p) {
        if (ptr_)
            ptr_->AddRef();
    }
    scoped_refptr(const scoped_refptr<T> &r) : ptr_(r.ptr_) {
        if (ptr_)
            ptr_->AddRef();
    }
    ~scoped_refptr() {
        if (ptr_)
            ptr_->Release();
    }

    T *get() const { return ptr_; }
    explicit operator bool() const { return ptr_ != nullptr; }
    T &operator*() const { return *ptr_; }
    T *operator->() const { return ptr_; }

    scoped_refptr<T> &operator=(const scoped_refptr<T> &r) { return *this = r.ptr_; }
    scoped_refptr<T> &operator=(T *p) {
        // AddRef first so that self assignment works.
        if (p)
            p->AddRef();
        if (ptr_)
            ptr_->Release();
        ptr_ = p;
        return *this;
    }

    // Returns the raw pointer without touching the reference count; the caller
    // owns one reference.
    T *release() {
        T *p = ptr_;
        ptr_ = nullptr;
        return p;
    }

   protected:
    T *ptr_;
};

}  // namespace webrtc

namespace rtc {

using webrtc::RefCountInterface;
using webrtc::RefCountReleaseStatus;

template <typename T>
using scoped_refptr = webrtc::scoped_refptr<T>;

}  // namespace rtc

namespace cricket {

enum MediaType { MEDIA_TYPE_AUDIO, MEDIA_TYPE_VIDEO, MEDIA_TYPE_DATA, MEDIA_TYPE_UNSUPPORTED };

}  // namespace cricket

namespace webrtc {

class FrameEncryptorInterface : public rtc::RefCountInterface {
   public:
    ~FrameEncryptorInterface() override {}

    // Writes the encrypted frame into encrypted_frame (which is guaranteed to
    // be at least GetMaxCiphertextByteSize(media_type, frame.size()) bytes) and
    // sets *bytes_written. Returns 0 on success, any other value on error (the
    // frame is then dropped).
    virtual int Encrypt(cricket::MediaType media_type,
                        uint32_t ssrc,
                        rtc::ArrayView<const uint8_t> additional_data,
                        rtc::ArrayView<const uint8_t> frame,
                        rtc::ArrayView<uint8_t> encrypted_frame,
                        size_t *bytes_written) = 0;

    virtual size_t GetMaxCiphertextByteSize(cricket::MediaType media_type, size_t frame_size) = 0;
};

class FrameDecryptorInterface : public rtc::RefCountInterface {
   public:
    enum class Status { kOk, kRecoverable, kFailedToDecrypt, kUnknown };

    struct Result {
        Result(Status status, size_t bytes_written) : status(status), bytes_written(bytes_written) {}

        bool IsOk() const { return status == Status::kOk; }

        const Status status;
        const size_t bytes_written;
    };

    ~FrameDecryptorInterface() override {}

    // Writes the decrypted frame into frame (which is guaranteed to be at least
    // GetMaxPlaintextByteSize(media_type, encrypted_frame.size()) bytes).
    // kRecoverable drops the frame while keeping the receive stream alive.
    virtual Result Decrypt(cricket::MediaType media_type,
                           const std::vector<uint32_t> &csrcs,
                           rtc::ArrayView<const uint8_t> additional_data,
                           rtc::ArrayView<const uint8_t> encrypted_frame,
                           rtc::ArrayView<uint8_t> frame) = 0;

    virtual size_t GetMaxPlaintextByteSize(cricket::MediaType media_type, size_t encrypted_frame_size) = 0;
};

}  // namespace webrtc

#endif  // E2EE_FRAME_CRYPTOR_ABI_H_
