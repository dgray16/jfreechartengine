/*
* This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.libreplan.java.zk.components;

import java.awt.Color;
import java.awt.Font;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.encoders.EncoderUtil;
import org.jfree.chart.encoders.ImageFormat;
import org.jfree.chart.entity.CategoryItemEntity;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.JFreeChartEntity;
import org.jfree.chart.entity.LegendItemEntity;
import org.jfree.chart.entity.PieSectionEntity;
import org.jfree.chart.entity.PlotEntity;
import org.jfree.chart.entity.TickLabelEntity;
import org.jfree.chart.entity.TitleEntity;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.gantt.GanttCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.data.xy.XYZDataset;
import org.zkoss.lang.Objects;
import org.zkoss.util.TimeZones;
import org.zkoss.zk.ui.UiException;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Area;
import org.zkoss.zul.CategoryModel;
import org.zkoss.zul.Chart;
import org.zkoss.zul.ChartModel;
import org.zkoss.zul.PieModel;
import org.zkoss.zul.XYModel;
import org.zkoss.zul.impl.ChartEngine;

/**
 * It is own ChartEngine for LibrePlan.
 * Used for Gantt charts.
 *
 * @author Farruco Sanjurjo
 * @author Nacho Barrientos
 * @author Vova Perebykivskyi <vova@libreplan-enterprise.com>
 */
public class JFreeChartEngine implements ChartEngine {

    private final String _LEGEND_SEQ_ATTR = "LEGEND_SEQ";

    private final String _TICK_SEQ_ATTR = "TICK_SEQ";

    private final String _SERIES_ATTR = "series";

    private final String _CATEGORY_ATTR = "category";

    private final String _ENTITY_ATTR = "entity";

    private final String _LEGEND_ATTR = "LEGEND";

    private final String _TITLE_ATTR = "TITLE";

    private String _type;

    private ChartImpl _chartImpl;

    private transient boolean _threeD;

    private static final SimpleDateFormat _dateFormat = new SimpleDateFormat();

    private static Map _periodMap = new HashMap(10);

    static {
        _periodMap.put(Chart.MILLISECOND, org.jfree.data.time.Millisecond.class);
        _periodMap.put(Chart.SECOND, org.jfree.data.time.Second.class);
        _periodMap.put(Chart.MINUTE, org.jfree.data.time.Minute.class);
        _periodMap.put(Chart.HOUR, org.jfree.data.time.Hour.class);
        _periodMap.put(Chart.DAY, org.jfree.data.time.Day.class);
        _periodMap.put(Chart.WEEK, org.jfree.data.time.Week.class);
        _periodMap.put(Chart.MONTH, org.jfree.data.time.Month.class);
        _periodMap.put(Chart.QUARTER, org.jfree.data.time.Quarter.class);
        _periodMap.put(Chart.YEAR, org.jfree.data.time.Year.class);
    }

    private ChartImpl getChartImpl(Chart chart){
        if (Objects.equals(chart.getType(), _type) && _threeD == chart.isThreeD()) {
            return _chartImpl;
        }

        if ( Chart.TIME_SERIES.equals(chart.getType()) ) {
            _chartImpl = new TimeSeriesChart();
        }else if ( Chart.BAR.equals(chart.getType())) {
            _chartImpl = chart.isThreeD() ? new Bar3dChart() : new BarChart();
        } else if (Chart.PIE.equals(chart.getType())) {
            _chartImpl = chart.isThreeD() ? new Pie3dChart() : new PieChart();
        } else {
            throw new RuntimeException("Unsupported chart type: " + chart.getType());
        }

        _threeD = chart.isThreeD();
        _type = chart.getType();

        return _chartImpl;
    }

    public byte[] drawChart(Object data) {
        Chart chart = (Chart) data;
        ChartImpl impl = getChartImpl(chart);
        JFreeChart jfchart = impl.createChart(chart);

        Plot plot = jfchart.getPlot();
        float alpha = ((float) chart.getFgAlpha()) / 255;
        plot.setForegroundAlpha(alpha);

        alpha = ((float) chart.getBgAlpha()) / 255;
        plot.setBackgroundAlpha(alpha);

        int[] bgRGB = chart.getBgRGB();
        if (bgRGB != null) {
            plot.setBackgroundPaint(new Color(bgRGB[0], bgRGB[1], bgRGB[2], chart.getBgAlpha()));
        }

        int[] paneRGB = chart.getPaneRGB();
        if (paneRGB != null) {
            jfchart.setBackgroundPaint(new Color(paneRGB[0], paneRGB[1], paneRGB[2], chart.getPaneAlpha()));
        }

        /*
         * Since 3.6.3,
         * JFreeChart 1.0.13 change default fonts which does not support Chinese, allow developer to set font.
         */

        // Title font
        final Font tfont = chart.getTitleFont();
        if (tfont != null) {
            jfchart.getTitle().setFont(tfont);
        }

        // Legend font
        final Font lfont = chart.getLegendFont();
        if (lfont != null) {
            jfchart.getLegend().setItemFont(lfont);
        }

        if (plot instanceof CategoryPlot) {
            final CategoryPlot cplot = (CategoryPlot) plot;
            cplot.setRangeGridlinePaint(new Color(0xc0, 0xc0, 0xc0));

            // Domain axis ( x axis )
            final Font xlbfont = chart.getXAxisFont();
            final Font xtkfont = chart.getXAxisTickFont();

            if (xlbfont != null) {
                cplot.getDomainAxis().setLabelFont(xlbfont);
            }

            if (xtkfont != null) {
                cplot.getDomainAxis().setTickLabelFont(xtkfont);
            }

            Color[] colorMappings = (Color[])chart.getAttribute("series-color-mappings");
            if (colorMappings != null) {
                for (int ii=0; ii < colorMappings.length; ii++) {
                    cplot.getRenderer().setSeriesPaint(ii, colorMappings[ii]);
                }
            }

            Double lowerBound = (Double)chart.getAttribute("range-axis-lower-bound");
            if (lowerBound != null) {
                cplot.getRangeAxis().setAutoRange(false);
                cplot.getRangeAxis().setLowerBound(lowerBound);
            }

            Double upperBound = (Double)chart.getAttribute("range-axis-upper-bound");
            if (upperBound != null) {
                cplot.getRangeAxis().setAutoRange(false);
                cplot.getRangeAxis().setUpperBound(upperBound);
            }

            // Range axis ( y axis )
            final Font ylbfont = chart.getYAxisFont();
            final Font ytkfont = chart.getYAxisTickFont();

            if (ylbfont != null) {
                cplot.getRangeAxis().setLabelFont(ylbfont);
            }

            if (ytkfont != null) {
                cplot.getRangeAxis().setTickLabelFont(ytkfont);
            }

        } else if (plot instanceof XYPlot) {
            final XYPlot xyplot = (XYPlot) plot;
            xyplot.setRangeGridlinePaint(Color.LIGHT_GRAY);
            xyplot.setDomainGridlinePaint(Color.LIGHT_GRAY);

            // Domain axis ( x axis )
            final Font xlbfont = chart.getXAxisFont();
            final Font xtkfont = chart.getXAxisTickFont();

            if (xlbfont != null) {
                xyplot.getDomainAxis().setLabelFont(xlbfont);
            }

            if (xtkfont != null) {
                xyplot.getDomainAxis().setTickLabelFont(xtkfont);
            }

            // Range axis ( y axis )
            final Font ylbfont = chart.getYAxisFont();
            final Font ytkfont = chart.getYAxisTickFont();

            if (ylbfont != null) {
                xyplot.getRangeAxis().setLabelFont(ylbfont);
            }

            if (ytkfont != null) {
                xyplot.getRangeAxis().setTickLabelFont(ytkfont);
            }

        } else if (plot instanceof PiePlot) {
            plot.setOutlineStroke(null);
        }

        // Callbacks for each area
        ChartRenderingInfo jfinfo = new ChartRenderingInfo();

        BufferedImage bi = jfchart.createBufferedImage(
                chart.getIntWidth(), chart.getIntHeight(), Transparency.TRANSLUCENT, jfinfo);

        // Remove old areas
        if (chart.getChildren().size() > 20)
            chart.invalidate(); // Improve performance if too many chart

        chart.getChildren().clear();

        if (Events.isListened(chart, Events.ON_CLICK, false) || chart.isShowTooltiptext()) {
            int j = 0;
            String preUrl = null;

            for ( Iterator it = jfinfo.getEntityCollection().iterator(); it.hasNext(); ) {
                ChartEntity ce = ( ChartEntity ) it.next();
                final String url = ce.getURLText();

                // Workaround JFreeChart's bug (skip replicate areas)
                if ( url != null ) {
                    if ( preUrl == null ) {
                        preUrl = url;
                    } else if (url.equals(preUrl)) { // Start replicate, skip
                        break;
                    }
                }

                /*
                 * 1. JFreeChartEntity area cover the whole chart, will "mask" other areas.
                 * 2. LegendTitle area cover the whole legend, will "mask" each legend.
                 * 3. PlotEntity cover the whole chart plotting araa, will "mask" each bar/line/area.
                 */

                if ( !(ce instanceof JFreeChartEntity) &&
                        !(ce instanceof TitleEntity && ((TitleEntity)ce).getTitle() instanceof LegendTitle) &&
                        !(ce instanceof PlotEntity) ) {

                    Area area = new Area();
                    area.setParent(chart);
                    area.setCoords(ce.getShapeCoords());
                    area.setShape(ce.getShapeType());
                    area.setId("area_"+chart.getId()+'_'+(j++));

                    if (chart.isShowTooltiptext() && ce.getToolTipText() != null) {
                        area.setTooltiptext(ce.getToolTipText());
                    }

                    area.setAttribute("url", ce.getURLText());
                    impl.render(chart, area, ce);

                    if (chart.getAreaListener() != null) {
                        try {
                            chart.getAreaListener().onRender(area, ce);
                        } catch (Exception ex) {
                            throw UiException.Aide.wrap(ex);
                        }
                    }
                }
            }
        }

        /*
         * Clean up the "LEGEND_SEQ".
         * Used for workaround LegendItemEntity.getSeries() always return 0.
         * Used for workaround TickLabelEntity no information.
         */
        chart.removeAttribute(_LEGEND_SEQ_ATTR);
        chart.removeAttribute(_TICK_SEQ_ATTR);

        try {
            // Encode into png image format byte array
            return EncoderUtil.encode(bi, ImageFormat.PNG, true);

        } catch(java.io.IOException ex) {
            throw UiException.Aide.wrap(ex);
        }
    }

    private void decodeLegendInfo(Area area, LegendItemEntity info, Chart chart) {
        if (info == null)
            return;

        final ChartModel model = chart.getModel();
        final int seq = (Integer) chart.getAttribute(_LEGEND_SEQ_ATTR);

        if (model instanceof CategoryModel) {
            Comparable series = ((CategoryModel)model).getSeries(seq);
            area.setAttribute(_SERIES_ATTR, series);

            if (chart.isShowTooltiptext() && info.getToolTipText() == null) {
                area.setTooltiptext(series.toString());
            }

        } else if (model instanceof XYModel) {
            Comparable series = ((XYModel)model).getSeries(seq);
            area.setAttribute(_SERIES_ATTR, series);

            if (chart.isShowTooltiptext() && info.getToolTipText() == null) {
                area.setTooltiptext(series.toString());
            }
        }
    }

    /**
     * Decode XYItemEntity into key-value pair of Area's componentScope.
     */
    private void decodeXYInfo(Area area, XYItemEntity info) {
        if (info == null) {
            return;
        }

        XYDataset dataset = info.getDataset();
        int si = info.getSeriesIndex();
        int ii = info.getItem();

        area.setAttribute(_SERIES_ATTR, dataset.getSeriesKey(si));

        if (dataset instanceof XYZDataset) {
            XYZDataset ds = (XYZDataset) dataset;
            area.setAttribute("x", ds.getX(si, ii));
            area.setAttribute("y", ds.getY(si, ii));
            area.setAttribute("z", ds.getZ(si, ii));
        } else {
            area.setAttribute("x", dataset.getX(si, ii));
            area.setAttribute("y", dataset.getY(si, ii));
        }

    }

    /**
     * Transfer a CategoryModel into JFreeChart CategoryDataset.
     */
    private CategoryDataset categoryModelToCategoryDataset(CategoryModel model) {
        final DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        for (final Iterator it = model.getKeys().iterator(); it.hasNext();) {
            final List key = (List) it.next();
            Comparable series = (Comparable) key.get(0);
            Comparable category = (Comparable) key.get(1);
            Number value = model.getValue(series, category);
            dataset.setValue(value, series, category);
        }
        return dataset;
    }

    /**
     * Transfer a PieModel into JFreeChart PieDataset.
     */
    private PieDataset pieModelToPieDataset(PieModel model) {
        final DefaultPieDataset dataset = new DefaultPieDataset();

        for (final Iterator it = model.getCategories().iterator(); it.hasNext();) {
            final Comparable category = (Comparable)it.next();
            Number value = model.getValue(category);
            dataset.setValue(category, value);
        }

        return dataset;
    }

    private PlotOrientation getOrientation(String orient) {
        return "horizontal".equals(orient) ? PlotOrientation.HORIZONTAL : PlotOrientation.VERTICAL;
    }

    /**
     * base chart
     * Chart specific implementation.
     */
    private abstract class ChartImpl {

        abstract void render(Chart chart, Area area, ChartEntity info);

        abstract JFreeChart createChart(Chart chart);
    }


    private class TimeSeriesChart extends ChartImpl {

        @Override
        public void render(Chart chart, Area area, ChartEntity info) {
            if (info instanceof LegendItemEntity) {
                renderLegendItemEntity(area, chart, info);

            } else if (info instanceof XYItemEntity) {
                area.setAttribute(_ENTITY_ATTR, "DATA");
                decodeXYInfo(area, (XYItemEntity) info);

            } else {
                area.setAttribute(_ENTITY_ATTR, _TITLE_ATTR);
                if (chart.isShowTooltiptext()) {
                    area.setTooltiptext(chart.getTitle());
                }
            }
        }

        @Override
        public JFreeChart createChart(Chart chart) {
            ChartModel model = chart.getModel();

            if (!(model instanceof XYModel)) {
                throw new UiException("model must be a org.zkoss.zul.XYModel");
            }

            final JFreeChart jchart = ChartFactory.createTimeSeriesChart(
                    chart.getTitle(),
                    chart.getXAxis(),
                    chart.getYAxis(),
                    xyModelToTimeDataset((XYModel) model, chart),
                    chart.isShowLegend(),
                    chart.isShowTooltiptext(),
                    true);

            setupDateAxis(jchart, chart);

            return jchart;
        }

        /**
         * Transfer a XYModel into JFreeChart TimeSeriesCollection.
         */
        private XYDataset xyModelToTimeDataset(XYModel model, Chart chart) {
            TimeZone tz = chart.getTimeZone();
            if (tz == null)
                tz = TimeZones.getCurrent();

            String p = chart.getPeriod();
            if (p == null)
                p = Chart.MILLISECOND;

            Class pclass = (Class) _periodMap.get(p);
            if (pclass == null) {
                throw new UiException("Unsupported period for Time Series chart: "+p);
            }

            final TimeSeriesCollection dataset = new TimeSeriesCollection(tz);

            for (Comparable<?> series : model.getSeries()) {
                final TimeSeries tser = new TimeSeries(series);
                final int size = model.getDataCount(series);

                for (int j = 0; j < size; ++j) {

                    final RegularTimePeriod period =
                            RegularTimePeriod.createInstance(pclass, new Date(model.getX(series, j).longValue()), tz);

                    tser.addOrUpdate(period, model.getY(series, j));
                }

                dataset.addSeries(tser);
            }
            return dataset;
        }

        private void setupDateAxis(JFreeChart jchart, Chart chart) {
            final Plot plot = jchart.getPlot();
            final DateAxis axisX = (DateAxis) ((XYPlot) plot).getDomainAxis();
            final TimeZone zone = chart.getTimeZone();

            if (zone != null) {
                axisX.setTimeZone(zone);
            }

            if (chart.getDateFormat() != null) {
                axisX.setDateFormatOverride(_dateFormat);
            }
        }
    }

    private class BarChart extends ChartImpl {

        @Override
        public void render(Chart chart, Area area, ChartEntity info) {
            if (info instanceof LegendItemEntity) {
                renderLegendItemEntity(area, chart, info);

            } else if (info instanceof CategoryItemEntity) {
                area.setAttribute(_ENTITY_ATTR, "DATA");
                decodeCategoryInfo(area, (CategoryItemEntity)info);

            } else if (info instanceof XYItemEntity) {
                area.setAttribute(_ENTITY_ATTR, "DATA");
                decodeXYInfo(area, (XYItemEntity) info);

            } else if (info instanceof TickLabelEntity) {
                area.setAttribute(_ENTITY_ATTR, "CATEGORY");
                Integer seq = (Integer)chart.getAttribute(_TICK_SEQ_ATTR);
                seq = seq == null ? new Integer(0) : new Integer(seq + 1);
                chart.setAttribute(_TICK_SEQ_ATTR, seq);
                decodeTickLabelInfo(area, (TickLabelEntity) info, chart);

            } else {
                area.setAttribute(_ENTITY_ATTR, _TITLE_ATTR);
                if (chart.isShowTooltiptext()) {
                    area.setTooltiptext(chart.getTitle());
                }
            }
        }

        /**
         * Decode CategoryItemEntity into key-value pair of Area's componentScope.
         */
        private void decodeCategoryInfo(Area area, CategoryItemEntity info) {
            if (info == null) {
                return;
            }

            CategoryDataset dataset = info.getDataset();
            Comparable category = info.getColumnKey();
            Comparable series = info.getRowKey();

            area.setAttribute(_SERIES_ATTR, series);
            area.setAttribute(_CATEGORY_ATTR, category);

            if (dataset instanceof GanttCategoryDataset) {
                final GanttCategoryDataset gd = (GanttCategoryDataset) dataset;
                area.setAttribute("start", gd.getStartValue(series, category));
                area.setAttribute("end", gd.getEndValue(series, category));
                area.setAttribute("percent", gd.getPercentComplete(series, category));
            } else {
                area.setAttribute("value", dataset.getValue(series, category));
            }
        }

        /**
         * Decode TickLabelEntity into key-value pair of Area's componentScope.
         */
        private void decodeTickLabelInfo(Area area, TickLabelEntity info, Chart chart) {
            if (info == null) {
                return;
            }

            final ChartModel model = chart.getModel();
            final int seq = (Integer) chart.getAttribute(_TICK_SEQ_ATTR);

            if (model instanceof CategoryModel) {
                Comparable category = ((CategoryModel)model).getCategory(seq);
                area.setAttribute(_CATEGORY_ATTR, category);

                if (chart.isShowTooltiptext() && info.getToolTipText() == null) {
                    area.setTooltiptext(category.toString());
                }
            }
        }

        @Override
        public JFreeChart createChart(Chart chart) {
            ChartModel model = chart.getModel();

            if (model instanceof CategoryModel) {

                return ChartFactory.createBarChart(
                        chart.getTitle(),
                        chart.getXAxis(),
                        chart.getYAxis(),
                        categoryModelToCategoryDataset((CategoryModel) model),
                        getOrientation(chart.getOrient()),
                        chart.isShowLegend(),
                        chart.isShowTooltiptext(),
                        true);

            } else if (model instanceof XYModel) {

                return ChartFactory.createXYBarChart(
                        chart.getTitle(),
                        chart.getXAxis(),
                        false,
                        chart.getYAxis(),
                        (IntervalXYDataset) xyModelToXYDataset((XYModel) model),
                        getOrientation(chart.getOrient()),
                        chart.isShowLegend(),
                        chart.isShowTooltiptext(),
                        true);
            } else {
                throw new UiException("The only supported model is org.zkoss.zul.CategoryModel");
            }
        }

        /**
         * Transfer a XYModel into JFreeChart XYSeriesCollection.
         */
        private XYDataset xyModelToXYDataset(XYModel model) {
            final XYSeriesCollection dataset = new XYSeriesCollection();
            for (Comparable<?> series : model.getSeries()) {
                XYSeries xyser = new XYSeries(series, model.isAutoSort());
                final int size = model.getDataCount(series);

                for (int j = 0; j < size; ++j) {
                    xyser.add(model.getX(series, j), model.getY(series, j), false);
                }
                dataset.addSeries(xyser);
            }
            return dataset;
        }
    }

    private class Bar3dChart extends BarChart {

        @Override
        public JFreeChart createChart(Chart chart) {
            ChartModel model = chart.getModel();
            if (!(model instanceof CategoryModel)) {
                throw new UiException("model must be a org.zkoss.zul.CategoryModel");
            }

            return ChartFactory.createBarChart3D(
                    chart.getTitle(),
                    chart.getXAxis(),
                    chart.getYAxis(),
                    categoryModelToCategoryDataset((CategoryModel) model),
                    getOrientation(chart.getOrient()),
                    chart.isShowLegend(),
                    chart.isShowTooltiptext(),
                    true);
        }
    }

    private class PieChart extends ChartImpl {

        @Override
        void render(Chart chart, Area area, ChartEntity info) {
            if (info instanceof LegendItemEntity) {
                renderLegendItemEntity(area, chart, info);

            } else if (info instanceof PieSectionEntity) {
                area.setAttribute(_ENTITY_ATTR, "DATA");
                decodePieSectionInfo(area, (PieSectionEntity) info);

            } else {
                area.setAttribute(_ENTITY_ATTR, _TITLE_ATTR);
                if (chart.isShowTooltiptext()) {
                    area.setTooltiptext(chart.getTitle());
                }
            }
        }

        /**
         * Decode PieSectionEntity into key-value pair of Area's componentScope.
         */
        private void decodePieSectionInfo(Area area, PieSectionEntity info) {
            PieDataset dataset = info.getDataset();
            Comparable category = info.getSectionKey();
            area.setAttribute("value", dataset.getValue(category));
            area.setAttribute(_CATEGORY_ATTR, category);
        }

        @Override
        public JFreeChart createChart(Chart chart) {
            PieModel model = (PieModel) chart.getModel();
            if (!(model instanceof PieModel)) {
                throw new UiException("model must be a org.zkoss.zul.PieModel");
            }

            return ChartFactory.createPieChart(
                    chart.getTitle(),
                    pieModelToPieDataset(model),
                    chart.isShowLegend(),
                    chart.isShowTooltiptext(),
                    false);
        }

    }

    private class Pie3dChart extends PieChart {

        @Override
        public JFreeChart createChart(Chart chart) {
            PieModel model = (PieModel) chart.getModel();
            if (!(model instanceof PieModel)) {
                throw new UiException("model must be a org.zkoss.zul.PieModel");
            }

            return ChartFactory.createPieChart3D(
                    chart.getTitle(),
                    pieModelToPieDataset(model),
                    chart.isShowLegend(),
                    chart.isShowTooltiptext(),
                    false);
        }
    }

    private void renderLegendItemEntity(Area area, Chart chart, ChartEntity info) {
        area.setAttribute(_ENTITY_ATTR, _LEGEND_ATTR);
        Integer seq = (Integer) chart.getAttribute(_LEGEND_SEQ_ATTR);
        seq = seq == null ? new Integer(0) : new Integer(seq + 1);
        chart.setAttribute(_LEGEND_SEQ_ATTR, seq);
        decodeLegendInfo(area, (LegendItemEntity) info, chart);
    }

}