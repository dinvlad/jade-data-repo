package common.utils;

import java.util.HashMap;
import java.util.Map;

public class LabelUtils {

  /**
   * Checks that a map of labels meets the requirements listed here:
   * https://cloud.google.com/resource-manager/docs/creating-managing-labels#requirements
   *
   * <p>If tryToSanitize is set to true, then it will try to truncate values and replace disallowed
   * characters. If tryToSanitze is set to false, then it will just throw an exception if it hits an
   * invalid label.
   *
   * @param labels
   * @param tryToSanitize
   * @return
   */
  public static Map<String, String> validateLabelMap(
      Map<String, String> labels, boolean tryToSanitize) {
    if (labels.size() > 64) {
      throw new IllegalArgumentException("Maximum number of labels is 64. Found " + labels.size());
    }

    Map<String, String> sanitizedLabels = tryToSanitize ? new HashMap<>() : null;
    for (Map.Entry<String, String> label : labels.entrySet()) {
      String key = label.getKey();
      String value = label.getValue();

      if (key == null || key.isEmpty()) {
        throw new IllegalArgumentException("Key cannot be null or empty string.");
      }

      String sanitizedKey = sanitizeLabel(key);
      String sanitizedValue = sanitizeLabel(value);
      if (!tryToSanitize) {
        if (!sanitizedKey.equals(key)) {
          throw new IllegalArgumentException(
              "Invalid key: " + key + ". Sanitized version: " + sanitizedKey);
        }
        if (!sanitizedValue.equals(value)) {
          throw new IllegalArgumentException(
              "Invalid value: " + value + ". Sanitized version: " + sanitizedValue);
        }
      } else {
        if (sanitizedLabels.get(sanitizedKey) != null) {
          throw new IllegalArgumentException("Duplicate keys after sanitizing: " + sanitizedKey);
        }
        sanitizedLabels.put(sanitizedKey, sanitizedValue);
      }
    }
    return tryToSanitize ? sanitizedLabels : labels;
  }

  /**
   * Tries to sanitize a label key or value to meet the requirements listed here:
   * https://cloud.google.com/resource-manager/docs/creating-managing-labels#requirements
   *
   * <p>This includes: - forcing to lower case - removing all characters except letters, numbers,
   * dash and underscore - truncating to a maximum of 63 characters - appending "x" if the first
   * character is not a letter
   *
   * <p>NOTE: This method does not correctly handle international characters, which are allowed in
   * labels.
   *
   * @param initialValue
   * @return
   */
  public static String sanitizeLabel(String initialValue) {
    String sanitizedValue = initialValue.toLowerCase().replaceAll("[^a-z0-9-_]", "");
    if (!sanitizedValue.substring(0, 1).matches("[a-z]")) {
      sanitizedValue = "x" + sanitizedValue;
    }

    if (sanitizedValue.length() > 63) {
      sanitizedValue = sanitizedValue.substring(0, 62);
    }

    return sanitizedValue;
  }
}
