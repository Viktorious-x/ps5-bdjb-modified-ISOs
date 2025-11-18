package org.ps5jb.sdk.include.sys;

import org.ps5jb.sdk.core.SdkException;
import org.ps5jb.sdk.include.sys.errno.BadFileDescriptorException;
import org.ps5jb.sdk.include.sys.errno.DeadlockException;
import org.ps5jb.sdk.include.sys.errno.InvalidSizeException;
import org.ps5jb.sdk.include.sys.errno.InvalidValueException;
import org.ps5jb.sdk.include.sys.errno.MemoryFaultException;
import org.ps5jb.sdk.include.sys.errno.NotFoundException;
import org.ps5jb.sdk.include.sys.errno.OperationNotPermittedException;
import org.ps5jb.sdk.include.sys.errno.OutOfMemoryException;
import org.ps5jb.sdk.lib.LibKernel;
import org.ps5jb.sdk.res.ErrorMessages;

/**
 * This class represents <code>include/sys/errno.h</code> from FreeBSD source.
 */
public class ErrNo {
    public static final String EPERM = "EPERM";
    public static final String ENOENT = "ENOENT";
    public static final String ESRCH = "ESRCH";
    public static final String EINTR = "EINTR";
    public static final String EIO = "EIO";
    public static final String ENXIO = "ENXIO";
    public static final String E2BIG = "E2BIG";
    public static final String ENOEXEC = "ENOEXEC";
    public static final String EBADF = "EBADF";
    public static final String ECHILD = "ECHILD";
    public static final String EDEADLK = "EDEADLK";
    public static final String ENOMEM = "ENOMEM";
    public static final String EACCES = "EACCES";
    public static final String EFAULT = "EFAULT";
    public static final String ENOTBLK = "ENOTBLK";
    public static final String EBUSY = "EBUSY";
    public static final String EEXIST = "EEXIST";
    public static final String EXDEV = "EXDEV";
    public static final String ENODEV = "ENODEV";
    public static final String ENOTDIR = "ENOTDIR";
    public static final String EISDIR = "EISDIR";
    public static final String EINVAL = "EINVAL";
    public static final String ENFILE = "ENFILE";
    public static final String EMFILE = "EMFILE";
    public static final String ENOTTY = "ENOTTY";
    public static final String ETXTBSY = "ETXTBSY";
    public static final String EFBIG = "EFBIG";
    public static final String ENOSPC = "ENOSPC";
    public static final String ESPIPE = "ESPIPE";
    public static final String EROFS = "EROFS";
    public static final String EMLINK = "EMLINK";
    public static final String EPIPE = "EPIPE";
    public static final String EDOM = "EDOM";
    public static final String ERANGE = "ERANGE";
    public static final String EAGAIN = "EAGAIN";
    public static final String EWOULDBLOCK = "EWOULDBLOCK";
    public static final String EINPROGRESS = "EINPROGRESS";
    public static final String EALREADY = "EALREADY";
    public static final String ENOTSOCK = "ENOTSOCK";
    public static final String EDESTADDRREQ = "EDESTADDRREQ";
    public static final String EMSGSIZE = "EMSGSIZE";
    public static final String EPROTOTYPE = "EPROTOTYPE";
    public static final String ENOPROTOOPT = "ENOPROTOOPT";
    public static final String EPROTONOSUPPORT = "EPROTONOSUPPORT";
    public static final String ESOCKTNOSUPPORT = "ESOCKTNOSUPPORT";
    public static final String EOPNOTSUPP = "EOPNOTSUPP";
    public static final String ENOTSUP = "ENOTSUP";
    public static final String EPFNOSUPPORT = "EPFNOSUPPORT";
    public static final String EAFNOSUPPORT = "EAFNOSUPPORT";
    public static final String EADDRINUSE = "EADDRINUSE";
    public static final String EADDRNOTAVAIL = "EADDRNOTAVAIL";
    public static final String ENETDOWN = "ENETDOWN";
    public static final String ENETUNREACH = "ENETUNREACH";
    public static final String ENETRESET = "ENETRESET";
    public static final String ECONNABORTED = "ECONNABORTED";
    public static final String ECONNRESET = "ECONNRESET";
    public static final String ENOBUFS = "ENOBUFS";
    public static final String EISCONN = "EISCONN";
    public static final String ENOTCONN = "ENOTCONN";
    public static final String ESHUTDOWN = "ESHUTDOWN";
    public static final String ETOOMANYREFS = "ETOOMANYREFS";
    public static final String ETIMEDOUT = "ETIMEDOUT";
    public static final String ECONNREFUSED = "ECONNREFUSED";
    public static final String ELOOP = "ELOOP";
    public static final String ENAMETOOLONG = "ENAMETOOLONG";
    public static final String EHOSTDOWN = "EHOSTDOWN";
    public static final String EHOSTUNREACH = "EHOSTUNREACH";
    public static final String ENOTEMPTY = "ENOTEMPTY";
    public static final String EPROCLIM = "EPROCLIM";
    public static final String EUSERS = "EUSERS";
    public static final String EDQUOT = "EDQUOT";
    public static final String ESTALE = "ESTALE";
    public static final String EREMOTE = "EREMOTE";
    public static final String EBADRPC = "EBADRPC";
    public static final String ERPCMISMATCH = "ERPCMISMATCH";
    public static final String EPROGUNAVAIL = "EPROGUNAVAIL";
    public static final String EPROGMISMATCH = "EPROGMISMATCH";
    public static final String EPROCUNAVAIL = "EPROCUNAVAIL";
    public static final String ENOLCK = "ENOLCK";
    public static final String ENOSYS = "ENOSYS";
    public static final String EFTYPE = "EFTYPE";
    public static final String EAUTH = "EAUTH";
    public static final String ENEEDAUTH = "ENEEDAUTH";
    public static final String EIDRM = "EIDRM";
    public static final String ENOMSG = "ENOMSG";
    public static final String EOVERFLOW = "EOVERFLOW";
    public static final String ECANCELED = "ECANCELED";
    public static final String EILSEQ = "EILSEQ";
    public static final String ENOATTR = "ENOATTR";
    public static final String EDOOFUS = "EDOOFUS";
    public static final String EBADMSG = "EBADMSG";
    public static final String EMULTIHOP = "EMULTIHOP";
    public static final String ENOLINK = "ENOLINK";
    public static final String EPROTO = "EPROTO";
    public static final String ENOTCAPABLE = "ENOTCAPABLE";
    public static final String ECAPMODE = "ECAPMODE";
    public static final String ENOTRECOVERABLE = "ENOTRECOVERABLE";
    public static final String EOWNERDEAD = "EOWNERDEAD";

    /** All known FreeBSD error code constants. The order in the array respects the numeric value of the FreeBSD constant */
    private static final String[] errorCodes = new String[] {
        EPERM,
        ENOENT,
        ESRCH,
        EINTR,
        EIO,
        ENXIO,
        E2BIG,
        ENOEXEC,
        EBADF,
        ECHILD,
        EDEADLK,
        ENOMEM,
        EACCES,
        EFAULT,
        ENOTBLK,
        EBUSY,
        EEXIST,
        EXDEV,
        ENODEV,
        ENOTDIR,
        EISDIR,
        EINVAL,
        ENFILE,
        EMFILE,
        ENOTTY,
        ETXTBSY,
        EFBIG,
        ENOSPC,
        ESPIPE,
        EROFS,
        EMLINK,
        EPIPE,
        EDOM,
        ERANGE,
        EAGAIN,
        EWOULDBLOCK,
        EINPROGRESS,
        EALREADY,
        ENOTSOCK,
        EDESTADDRREQ,
        EMSGSIZE,
        EPROTOTYPE,
        ENOPROTOOPT,
        EPROTONOSUPPORT,
        ESOCKTNOSUPPORT,
        EOPNOTSUPP,
        ENOTSUP,
        EPFNOSUPPORT,
        EAFNOSUPPORT,
        EADDRINUSE,
        EADDRNOTAVAIL,
        ENETDOWN,
        ENETUNREACH,
        ENETRESET,
        ECONNABORTED,
        ECONNRESET,
        ENOBUFS,
        EISCONN,
        ENOTCONN,
        ESHUTDOWN,
        ETOOMANYREFS,
        ETIMEDOUT,
        ECONNREFUSED,
        ELOOP,
        ENAMETOOLONG,
        EHOSTDOWN,
        EHOSTUNREACH,
        ENOTEMPTY,
        EPROCLIM,
        EUSERS,
        EDQUOT,
        ESTALE,
        EREMOTE,
        EBADRPC,
        ERPCMISMATCH,
        EPROGUNAVAIL,
        EPROGMISMATCH,
        EPROCUNAVAIL,
        ENOLCK,
        ENOSYS,
        EFTYPE,
        EAUTH,
        ENEEDAUTH,
        EIDRM,
        ENOMSG,
        EOVERFLOW,
        ECANCELED,
        EILSEQ,
        ENOATTR,
        EDOOFUS,
        EBADMSG,
        EMULTIHOP,
        ENOLINK,
        EPROTO,
        ENOTCAPABLE,
        ECAPMODE,
        ENOTRECOVERABLE,
        EOWNERDEAD
    };

    /**
     * Returns the numeric value from the string error constant.
     *
     * @param errNo One of the error constants defined in this class.
     * @return Numeric value for the error constant. If constant cannot be found, 0 is returned.
     */
    public static int ord(String errNo) {
        int result = 0;
        for (int i = 0; i < errorCodes.length; ++i) {
            if (errorCodes[i].equals(errNo)) {
                result = i + 1;
                break;
            }
        }
        return result;
    }

    private final LibKernel libKernel;

    /**
     * Constructor.
     *
     * @param libKernel Instance of the 'libkernel' native library wrapper.
     */
    public ErrNo(LibKernel libKernel) {
        this.libKernel = libKernel;
    }

    /**
     * When a system call detects an error, it returns an integer value
     * indicating failure (usually -1) and sets the variable errno accordingly.
     * (This allows interpretation of the failure on receiving a -1
     * and to take action accordingly.)
     * Successful calls never set <code>errno</code>; once set,
     * it remains until another error occurs. It should only be examined after an error.
     * Note that a number of system calls overload the meanings
     * of these error numbers, and that the meanings must be interpreted
     * according to the type and circumstances of the call.
     *
     * @return Error code number. String value corresponding to FreeBSD error constant
     *   can be retrieved using {@link #getLastError()}
     */
    public int errno() {
        return this.libKernel.__error().read4();
    }

    /**
     * Returns the string representation of the FreeBSD error constant for the last
     * system call error as returned by {@link #errno()}. If the error number could
     * not be converted to string, then the numeric value of the error code as
     * string is returned.
     *
     * @return String error code representation. When the known error is returned, it
     *   will be the instance of one of the static constants declared in this class.
     *   This means that the error code can be compared directly by "==" operator
     *   rather than using <code>String.equals()</code> method.
     */
    public String getLastError() {
        int errno = errno();
        return (errno > 0 && errno <= errorCodes.length) ? errorCodes[errno - 1] : Integer.toString(errno);
    }

    /**
     * Returns the exception instance corresponding to the last error that occurred.
     * If last error type is unknown, return an instance {@link SdkException}. The
     * error message of the exception is retrieved using
     * {@link ErrorMessages#getClassErrorMessage(Class, String, Object...)}.
     *
     * @param clazz Owner class of the exception.
     * @param keySuffix Suffix used to derive the error message key.
     * @param formatArgs Optional formatting arguments inside the error message.
     * @return Subclass of <code>SdkException</code> which corresponds to the last
     *   occurred error. If error is unrecognized, an instance of <code>SdkException</code>
     *   is returned.
     */
    public SdkException getLastException(Class clazz, String keySuffix, Object ... formatArgs) {
        SdkException result;

        String lastError = getLastError();
        String errorMessageKey = keySuffix + "." + lastError;
        String errorMessage = ErrorMessages.getClassErrorMessage(clazz, errorMessageKey, formatArgs);
        if (lastError == ErrNo.EINVAL) {
            result = new InvalidValueException(errorMessage);
        } else if (lastError == ErrNo.EDEADLK) {
            result = new DeadlockException(errorMessage);
        } else if (lastError == ErrNo.EFAULT) {
            result = new MemoryFaultException(errorMessage);
        } else if (lastError == ErrNo.ESRCH) {
            result = new NotFoundException(errorMessage);
        } else if (lastError == ErrNo.ERANGE) {
            result = new InvalidSizeException(errorMessage);
        } else if (lastError == ErrNo.EPERM || lastError == ErrNo.EACCES) {
            result = new OperationNotPermittedException(errorMessage);
        } else if (lastError == ErrNo.EBADF) {
            result = new BadFileDescriptorException(errorMessage);
        } else if (lastError == ErrNo.ENOMEM) {
            result = new OutOfMemoryException(errorMessage);
        } else {
            result = new SdkException(ErrorMessages.getClassErrorMessage(clazz, keySuffix, lastError));
        }

        return result;
    }

    /**
     * Throws an exception corresponding to the last error code if the given
     * return value is -1.
     *
     * @param returnValue Actual return value.
     * @param clazz Class used to generate the exception message.
     * @param keySuffix Key used to generate the exception message.
     * @param formatArgs Arguments to use to generate the exception message.
     * @return Value of returnValue.
     * @throws SdkException If return value is not -1.
     */
    public long checkLastException(long returnValue, Class clazz, String keySuffix, Object ... formatArgs) throws SdkException {
        if (returnValue == -1) {
            throw getLastException(clazz, keySuffix, formatArgs);
        }
        return returnValue;
    }

    /**
     * Same as {@link #checkLastException(long, Class, String, Object...)} but for integer return types.
     *
     * @param returnValue Actual return value.
     * @param clazz Class used to generate the exception message.
     * @param keySuffix Key used to generate the exception message.
     * @param formatArgs Arguments to use to generate the exception message.
     * @return Value of returnValue.
     * @throws SdkException If return value is not -1.
     */
    public int checkLastException(int returnValue, Class clazz, String keySuffix, Object ... formatArgs) throws SdkException {
        return (int) checkLastException((long) returnValue, clazz, keySuffix, formatArgs);
    }

    /**
     * Same as {@link #checkLastException(long, Class, String, Object...)}
     * but uses a generic error message in case of an exception.
     *
     * @param returnValue Actual return value.
     * @return Value of returnValue.
     * @throws SdkException If return value is not -1.
     */
    public long checkLastException(long returnValue) throws SdkException {
        return checkLastException(returnValue, ErrNo.class, "checkLastException");
    }

    /**
     * Same as {@link #checkLastException(int, Class, String, Object...)}
     * but uses a generic error message in case of an exception.
     *
     * @param returnValue Actual return value.
     * @return Value of returnValue.
     * @throws SdkException If return value is not -1.
     */
    public int checkLastException(int returnValue) throws SdkException {
        return checkLastException(returnValue, ErrNo.class, "checkLastException");
    }
}
