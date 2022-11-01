package umu.software.activityrecognition.data.suppliers;

import java.util.function.Consumer;
import java.util.function.Supplier;

import umu.software.activityrecognition.data.dataframe.DataFrame;

/**
 * Supplier of data in the form of Dataframe.Row
 */
public interface DataSupplier extends Consumer<DataFrame.Row>
{

    /**
     * Returns the name of the supplier
     * @return the name of the supplier
     */
    String getName();


    /**
     * Perform initialization operations if necessary
     */
    void initialize();


    /**
     * Returns whether the supplier is ready to produce at least a row
     * @return whether the supplier is ready
     */
    boolean isReady();


    /**
     * Performs disposal operations if necessary. The supplier has to be ready again after initialize()
     */
    void dispose();

}
