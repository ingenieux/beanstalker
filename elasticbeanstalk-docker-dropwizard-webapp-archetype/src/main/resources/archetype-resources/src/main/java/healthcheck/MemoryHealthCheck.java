#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.healthcheck;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import br.com.ingenieux.dropwizard.guice.InjectableHealthCheck;
import io.dropwizard.setup.Environment;

public class MemoryHealthCheck extends InjectableHealthCheck {

  private static final Logger logger = LoggerFactory.getLogger(MemoryHealthCheck.class);

  private static final long MIN_TRIGGER_MEMORY = 1 * FileUtils.ONE_GB;

  @Inject
  Environment environment;

  @Override
  public String getName() {
    return "memory";
  }

  @Override
  protected Result check() throws Exception {
    if (SystemUtils.IS_OS_UNIX) {
      MetricRegistry registry = environment.metrics();
      Gauge max = registry.getGauges().get("jvm.memory.heap.max");
      Gauge used = registry.getGauges().get("jvm.memory.heap.used");

      Long usedAsLong = (Long) used.getValue();
      Long maxAsLong = (Long) max.getValue();

      double v = usedAsLong.doubleValue();
      double v1 = maxAsLong.doubleValue();
      double usedRatio = v / v1;

      logger
          .debug("available memory: {}/{} ({} used)", FileUtils.byteCountToDisplaySize(usedAsLong),
                 FileUtils.byteCountToDisplaySize(maxAsLong), 100 * usedRatio);

      if ((usedAsLong > MIN_TRIGGER_MEMORY) && (usedRatio > 0.9d)) {
        return Result.unhealthy("There's less than 10% of free memory");
      }
    }

    return Result.healthy();
  }
}
