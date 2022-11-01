package umu.software.activityrecognition.data.persistence;

import java.util.function.Function;

import umu.software.activityrecognition.data.dataframe.DataFrame;



public class DataFrameWriterFactory
{

    private DataFrameWriterFactory() {}

    /**
     * Creates a new writer to transform DataFrames into CSV documents
     * @param printColumnNames whether the first row should contain the column names
     * @param sep separator to divide cells
     * @return a writer function
     */
    public static Function<DataFrame, String> newToCSV(boolean printColumnNames, String sep)
    {
        return (df) -> {
            StringBuilder builder = new StringBuilder();
            if (printColumnNames)
                addCSVStringsToBuilder(builder, df.columns(), sep);

            df.forEachRowArray(objects -> {
                addCSVStringsToBuilder(builder, objects, sep);
                return null;
            });
            return builder.toString();
        };
    }

    /**
     * Creates a new writer to transform DataFrames into XML documents
     * (!) TODO
     * @return a new writer to transform DataFrames into XML documents
     */
    public static Function<DataFrame, String> newToXML()
    {
        return df -> {
            return "TODO"; //TODO
        };
    }


    private static void addCSVStringsToBuilder(StringBuilder builder, Object[] objs, String sep)
    {
        if (objs.length == 0)
            return;
        for (int i = 0; i < objs.length - 1; i++)
            builder
                    .append(String.format("%s", objs[i]))
                    .append(sep);
        builder.append(objs[objs.length-1]);
        builder.append(System.lineSeparator());
    }
}
