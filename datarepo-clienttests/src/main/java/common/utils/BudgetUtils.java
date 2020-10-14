package common.utils;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.billing.budgets.v1beta1.BillingAccountName;
import com.google.cloud.billing.budgets.v1beta1.Budget;
import com.google.cloud.billing.budgets.v1beta1.BudgetAmount;
import com.google.cloud.billing.budgets.v1beta1.BudgetServiceClient;
import com.google.cloud.billing.budgets.v1beta1.BudgetServiceSettings;
import com.google.cloud.billing.budgets.v1beta1.CreateBudgetRequest;
import com.google.cloud.billing.budgets.v1beta1.Filter;
import com.google.cloud.billing.budgets.v1beta1.ThresholdRule;
import com.google.protobuf.ListValue;
import com.google.protobuf.Value;
import com.google.type.Money;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import runner.config.ServiceAccountSpecification;

public class BudgetUtils {
  private static final Logger logger = LoggerFactory.getLogger(BudgetUtils.class);

  /**
   * Build a Google Budgeting client object with credentials for a given service account and project
   * id. The client object is newly created on each call to this method; it is not cached.
   */
  public static BudgetServiceClient getClientForServiceAccount(
      ServiceAccountSpecification serviceAccount, String projectId) throws Exception {
    GoogleCredentials serviceAccountCredentials =
        AuthenticationUtils.getServiceAccountCredential(serviceAccount);

    BudgetServiceSettings budgetServiceSettings =
        BudgetServiceSettings.newBuilder()
            .setCredentialsProvider(FixedCredentialsProvider.create(serviceAccountCredentials))
            .setQuotaProjectId(projectId)
            .build();
    BudgetServiceClient budgetServiceClient = BudgetServiceClient.create(budgetServiceSettings);
    return budgetServiceClient;
  }

  /**
   * Create a budget object, including any filters to apply to the spend data and any rules to use
   * for triggering alerts. Use the getBudget* helper methods in this class to build the various
   * objects passed to this method as arguments.
   */
  public static Budget createBudget(
      BudgetServiceClient budgetClient,
      String billingAccount,
      String budgetName,
      BudgetAmount budgetAmount,
      Filter budgetFilter,
      List<ThresholdRule> thresholdRules) {
    // build the create budget request. name and amount are required
    Budget.Builder budgetBuilder =
        Budget.newBuilder().setDisplayName(budgetName).setAmount(budgetAmount);
    if (budgetFilter != null) { // filter is optional
      budgetBuilder.setBudgetFilter(budgetFilter);
    }
    if (thresholdRules != null && thresholdRules.size() > 0) { // threshold rules are optional
      budgetBuilder.addAllThresholdRules(thresholdRules);
    }

    CreateBudgetRequest request =
        CreateBudgetRequest.newBuilder()
            .setParent(BillingAccountName.of(billingAccount).toString())
            .setBudget(budgetBuilder.build())
            .build();
    Budget budget = budgetClient.createBudget(request);
    logger.info(
        "Created budget: {}, {}",
        budget.getName(),
        budget.getAmount().getSpecifiedAmount().toString());

    return budget;
  }

  /**
   * Helper method to construct a BudgetAmount object. The currency code for US dollars is "USD".
   */
  public static BudgetAmount getBudgetAmount(long dollars, int cents, String currencyCode) {
    Money budgetAmountMoney =
        Money.newBuilder()
            .setUnits(dollars)
            .setNanos(cents * 10000000)
            .setCurrencyCode(currencyCode)
            .build();
    return BudgetAmount.newBuilder().setSpecifiedAmount(budgetAmountMoney).build();
  }

  /**
   * Helper map of service names to ids. These can be found under the Billing > Pricing console
   * page. This should probably be replaced with a call to the Cloud Billing API, filtering on the
   * service name or description.
   */
  public enum ServiceId {
    CLOUD_STORAGE("95FF-2EF5-5EA1"),
    BIGQUERY("24E6-581D-38E5"),
    CLOUD_FIRESTORE("D97E-AB26-5D95");

    private final String id;

    ServiceId(String id) {
      this.id = id;
    }

    public String getValue() {
      return id;
    }
  }

  /**
   * Helper method to construct a Filter object. All fields are optional. Note that the API allows
   * only one label key/value pair to filter on.
   */
  public static Filter getBudgetFilter(
      List<String> projectIds, List<ServiceId> serviceIds, Map<String, String> labels) {
    Filter.Builder filterBuilder = Filter.newBuilder();

    if (projectIds != null && projectIds.size() > 0) {
      // prepend the project ids with "projects/"
      List<String> convertedProjectIds = new ArrayList<>();
      for (String projectId : projectIds) {
        convertedProjectIds.add("projects/" + projectId);
      }
      filterBuilder.addAllProjects(convertedProjectIds);
    }

    if (serviceIds != null && serviceIds.size() > 0) {
      // prepend the service ids with "services/"
      List<String> convertedServiceIds = new ArrayList<>();
      for (ServiceId serviceId : serviceIds) {
        convertedServiceIds.add("services/" + serviceId.getValue());
      }
      filterBuilder.addAllServices(convertedServiceIds);
    }

    if (labels != null && labels.size() > 0) {
      if (labels.size() > 1) {
        throw new IllegalArgumentException("Only one label key/value pair allowed.");
      }

      // convert each label value to a ListValue
      Map<String, ListValue> convertedLabels = new HashMap<>();
      for (Map.Entry<String, String> label : labels.entrySet()) {
        ListValue convertedValue =
            ListValue.newBuilder()
                .addValues(Value.newBuilder().setStringValue(label.getValue()).build())
                .build();
        convertedLabels.put(label.getKey(), convertedValue);
      }
      filterBuilder.putAllLabels(convertedLabels);
    }

    return filterBuilder.build();
  }

  /**
   * Helper method to construct a ThresholdRule object. The fractionOfBudget argument is a number
   * between 0 and 1 (e.g. 0.1 = 10%).
   */
  public static ThresholdRule getBudgetThresholdRule(
      ThresholdRule.Basis basis, double fractionOfBudget) {
    return ThresholdRule.newBuilder()
        .setSpendBasis(basis)
        .setThresholdPercent(fractionOfBudget) // between 0 and 1
        .build();
  }
}
