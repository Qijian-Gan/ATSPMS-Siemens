package ATSPMs;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYSplineRenderer;
import java.awt.Color;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;

public class XYYLineChartSeriesCollection extends ApplicationFrame{
    // This function is used to draw XYY plots with two XYSeriesCollection inputs
    public XYYLineChartSeriesCollection(final String title,final String xLabel, final String yLabel1,final String yLabel2, final XYSeriesCollection dataset1,
                        final XYSeriesCollection dataset2) {
        super(title);

        //construct the plot
        XYPlot plot = new XYPlot();
        plot.setDataset(0, dataset1);
        plot.setDataset(1, dataset2);

        //customize the plot with renderers and axis
        plot.setRenderer(0, new XYSplineRenderer());//use default fill paint for first series
        plot.setRenderer(1, new XYSplineRenderer());
        plot.setRangeAxis(0, new NumberAxis(yLabel1));
        plot.setRangeAxis(1, new NumberAxis(yLabel2));
        plot.setDomainAxis(new NumberAxis(xLabel));

        //Map the data to the appropriate axis
        plot.mapDatasetToRangeAxis(0, 0);
        plot.mapDatasetToRangeAxis(1, 1);

        final StandardXYItemRenderer renderer = new StandardXYItemRenderer();
        renderer.setBaseShapesFilled(false);
        plot.setRenderer(0, renderer);

        //generate the chart
        JFreeChart chart = new JFreeChart(title, getFont(), plot, true);
        chart.setBackgroundPaint(Color.WHITE);
        final ChartPanel chartPanel = new ChartPanel(chart);
        setContentPane(chartPanel);
        //ChartFactory.setChartTheme(StandardChartTheme.createJFreeTheme());
        chartPanel.setPreferredSize(new java.awt.Dimension(1000, 500));
        final ApplicationFrame frame = new ApplicationFrame(title);
        frame.setContentPane(chartPanel);
        frame.pack();
        frame.setVisible(true);
    }
}
