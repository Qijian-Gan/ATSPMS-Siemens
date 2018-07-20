package ATSPMs;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYSplineRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import java.awt.*;

public class XYLineChart extends ApplicationFrame {
    // This function is used to plot line charts

    public XYLineChart(final String title,final String xLabel, final String yLabel, final XYSeries series) {
        super(title);

        final XYSeriesCollection data = new XYSeriesCollection(series);
        //construct the plot
        XYPlot plot = new XYPlot();
        plot.setDataset(data);
        plot.setRenderer(new XYSplineRenderer());
        plot.setRangeAxis(new NumberAxis(yLabel));
        plot.setDomainAxis(new NumberAxis(xLabel));

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
