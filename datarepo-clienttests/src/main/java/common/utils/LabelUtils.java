package common.utils;

import java.util.HashMap;
import java.util.Map;

public class LabelUtils {

  public static Map<String, String> sanitizeLabelMap(Map<String, String> labels) {
    Map<String, String> sanitizedLabelMap = new HashMap<>();
    for (Map.Entry<String, String> label : labels.entrySet()) {
      sanitizedLabelMap.put(sanitizeLabel(label.getKey()), sanitizeLabel(label.getValue()));
    }
    return sanitizedLabelMap;
  }

  public static String sanitizeLabel(String initialValue) {
    return initialValue.toLowerCase().replaceAll("[^a-z0-9-_]", "");
  }
}
