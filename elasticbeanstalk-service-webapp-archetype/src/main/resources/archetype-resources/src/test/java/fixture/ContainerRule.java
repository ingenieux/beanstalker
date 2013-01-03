#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.fixture;

import java.lang.reflect.Field;

import org.junit.internal.runners.statements.InvokeMethod;
import org.junit.internal.runners.statements.RunBefores;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

/**
 * Represents a jUnit 4.10+ Rule.
 * 
 * @author aldrin
 * 
 */
public class ContainerRule implements TestRule {
	public class ContainerRuleStatement extends Statement {
		private Statement base;

		private boolean initialized = false;

		public ContainerRuleStatement(Statement base, Description description) {
			this.base = base;
		}

		@Override
		public void evaluate() throws Throwable {
			boolean shouldInitializeP = (!initialized)
					&& (base instanceof RunBefores || base instanceof InvokeMethod);

			if (shouldInitializeP) {
				Injector injector = getInjector();

				Class<?> baseClass = base.getClass();

				Field fTargetRef = baseClass.getDeclaredField("fTarget");

				fTargetRef.setAccessible(true);

				Object target = fTargetRef.get(base);

				injector.injectMembers(target);

				initialized = true;
			}

			base.evaluate();
		}
	}

	private final Module module;

	@Override
	public Statement apply(Statement base, Description description) {
		return new ContainerRuleStatement(base, description);
	}

	protected Injector getInjector() {
		return Guice.createInjector(getModule());
	}

	public ContainerRule(Module module) {
		this.module = module;
	}

	public Module getModule() {
		return module;
	}
}
