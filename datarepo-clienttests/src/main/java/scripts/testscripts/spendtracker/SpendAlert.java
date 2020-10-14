package scripts.testscripts.spendtracker;

import com.google.cloud.billing.budgets.v1beta1.Budget;
import com.google.cloud.billing.budgets.v1beta1.BudgetAmount;
import com.google.cloud.billing.budgets.v1beta1.BudgetServiceClient;
import com.google.cloud.billing.budgets.v1beta1.Filter;
import com.google.cloud.billing.budgets.v1beta1.ThresholdRule;
import common.utils.BudgetUtils;
import common.utils.FileUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import runner.config.TestUserSpecification;

public class SpendAlert extends runner.TestScript {
  private static final Logger logger = LoggerFactory.getLogger(SpendAlert.class);

  protected static String projectId;

  public void setParameters(List<String> parameters) {
    if (parameters == null || parameters.size() < 1) {
      throw new IllegalArgumentException("Project id must be specified as the first parameter.");
    }
    projectId = parameters.get(0);
    logger.info("projectId = {}", projectId);
  }

  public void userJourney(TestUserSpecification testUser) throws Exception {
    try (BudgetServiceClient budgetClient =
        BudgetUtils.getClientForServiceAccount(server.testRunnerServiceAccount, projectId)) {

      // define the amount of the budget
      BudgetAmount budgetAmount = BudgetUtils.getBudgetAmount(1, 25, "USD");

      // define the filter to use on the spend data
      List<String> projectIds = Collections.singletonList(projectId);
      List<BudgetUtils.ServiceId> serviceIds = new ArrayList<>();
      serviceIds.add(BudgetUtils.ServiceId.CLOUD_STORAGE);
      serviceIds.add(BudgetUtils.ServiceId.BIGQUERY);
      Map<String, String> labels = new HashMap<>();
      labels.put("tdr-creator", "dumbledore");
      Filter budgetFilter = BudgetUtils.getBudgetFilter(projectIds, serviceIds, labels);

      // define the threshold rules after which emails or pub/sub messages will be triggered
      List<ThresholdRule> thresholdRules = new ArrayList<>();
      thresholdRules.add(
          BudgetUtils.getBudgetThresholdRule(ThresholdRule.Basis.CURRENT_SPEND, 0.1));
      thresholdRules.add(
          BudgetUtils.getBudgetThresholdRule(ThresholdRule.Basis.FORECASTED_SPEND, 0.25));

      Budget budget =
          BudgetUtils.createBudget(
              budgetClient,
              billingAccount,
              FileUtils.randomizeName("terra-budget"),
              budgetAmount,
              budgetFilter,
              thresholdRules);
      logger.info("Created budget: {}, {}", budget.getName(), budget.getAmount().toString());
    }
  }

  public void cleanup(List<TestUserSpecification> testUsers) throws Exception {
    // don't delete the budget because we want to see it apply to generated spend data
  }
}
