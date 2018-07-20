package ATSPMs;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYSplineRenderer;
import java.awt.Color;
import org.jfree.chart.axis.NumberAxis;

public class XYYLineChart extends ApplicationFrame {
    // This function is used to generate XYY line chart with two XYSeries

    public XYYLineChart(final String title,final String xLabel, final String yLabel1,final String yLabel2, final XYSeries series1,
                        final XYSeries series2) {
        super(title);

        //create the datasets
        XYSeriesCollection dataset1 = new XYSeriesCollection();
        XYSeriesCollection dataset2 = new XYSeriesCollection();
        dataset1.addSeries(series1);
        dataset2.addSeries(series2);

        //construct the plot
        XYPlot plot = new XYPlot();
        plot.setDataset(0, dataset1);
        plot.setDataset(1, dataset2);

        //customize the plot with renderers and axis
        plot.setRenderer(0, new XYSplineRenderer());
        XYSplineRenderer splinerenderer = new XYSplineRenderer();
        splinerenderer.setSeriesFillPaint(0, Color.BLUE);
        plot.setRenderer(1, splinerenderer);
        plot.setRangeAxis(0, new NumberAxis(yLabel1));
        plot.setRangeAxis(1, new NumberAxis(yLabel2));
        plot.setDomainAxis(new NumberAxis(xLabel));

        //Map the data to the appropriate axis
        plot.mapDatasetToRangeAxis(0, 0);
        plot.mapDatasetToRangeAxis(1, 1);

        //generate the chart
        JFreeChart chart = new JFreeChart(title, getFont(), plot, true);
        chart.setBackgroundPaint(Color.WHITE);
        final ChartPanel chartPanel = new ChartPanel(chart);
        setContentPane(chartPanel);
        ChartFactory.setChartTheme(StandardChartTheme.createJFreeTheme());
        chartPanel.setPreferredSize(new java.awt.Dimension(1000, 500));
        final ApplicationFrame frame = new ApplicationFrame(title);
        frame.setContentPane(chartPanel);
        frame.pack();
        frame.setVisible(true);
    }
}
