package com.nnp.kubernetes_integration.utils;

import com.nnp.kubernetes_integration.exceptions.K8sIntgException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class K8sConversionUtils {

    private final static Double BINARY_BASE_BYTES = 1024.00;
    private final static Double DECIMAL_BASE_BYTES = 1000.00;
    private static final long KIB = 1024L;
    private static final long MIB = KIB * 1024L;
    private static final long GIB = MIB * 1024L;
    public static BigDecimal parseMemoryToBytes(BigDecimal value, String format) {


        return switch (format) {
            case "Ki" -> value.multiply(BigDecimal.valueOf(KIB));
            case "Mi" -> value.multiply(BigDecimal.valueOf(MIB));
            case "Gi" -> value.multiply(BigDecimal.valueOf(GIB));
            default -> throw new K8sIntgException(
                    format + ": Format For Memory In K8s Not Handled",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        };
    }

    public static BigDecimal parseCpuToNanoCores(BigDecimal value, String format) {

        return switch (format) {
            case "n", "" -> value;
            case "m" -> value.multiply(BigDecimal.valueOf(Math.pow(DECIMAL_BASE_BYTES, 2)));
            case "u" -> value.multiply(BigDecimal.valueOf(Math.pow(DECIMAL_BASE_BYTES, 3)));
            default -> throw new K8sIntgException(format + ": Format For CPU Is Not Handled", HttpStatus.INTERNAL_SERVER_ERROR);
        };
    }

    private static final long KB = 1000L;
    private static final long MB = KB * 1000L;
    private static final long GB = MB * 1000L;

    public static BigDecimal convertBytesToFormat(BigDecimal value, String format) {
        return switch (format) {
            case "kb" -> value.divide(BigDecimal.valueOf(KB), RoundingMode.HALF_DOWN);
            case "mb" -> value.divide(BigDecimal.valueOf(MB), RoundingMode.HALF_DOWN);
            case "gb" -> value.divide(BigDecimal.valueOf(GB), RoundingMode.HALF_DOWN);
            default -> throw new K8sIntgException(
                    format + ": Format For Memory Not Handled",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        };
    }

    public static BigDecimal convertNanoCoresToFormat(BigDecimal value, String format){
        return switch (format) {
            case "n" -> value;
            case "m" -> value.divide(BigDecimal.valueOf(Math.pow(DECIMAL_BASE_BYTES, 2)), RoundingMode.HALF_DOWN);
            case "u" -> value.divide(BigDecimal.valueOf(Math.pow(DECIMAL_BASE_BYTES, 3)), RoundingMode.HALF_DOWN);
            default -> throw new K8sIntgException(format + ": Format For Cpu In K8s Not Handled", HttpStatus.INTERNAL_SERVER_ERROR);
        };
    }
}
