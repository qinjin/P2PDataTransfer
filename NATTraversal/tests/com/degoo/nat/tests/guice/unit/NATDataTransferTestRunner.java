package com.degoo.nat.tests.guice.unit;

import com.google.inject.Injector;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

import java.util.List;

public class NATDataTransferTestRunner extends BlockJUnit4ClassRunner {
	private final Injector injector;

	/**
	 * Creates a BlockJUnit4ClassRunner to run {@code klass}
	 *
	 * @throws org.junit.runners.model.InitializationError
	 *          if the test class is malformed.
	 */
	public NATDataTransferTestRunner(Class<?> classToRun) throws InitializationError {
		super(classToRun);
		injector = UnitTestInjectorFactory.createNATInjector();
	}

	@Override
    public Object createTest() {
        return injector.getInstance(getTestClass().getJavaClass());
    }

	@Override
    protected void validateZeroArgConstructor(List<Throwable> errors) {
    }
}
