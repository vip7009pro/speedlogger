package com.hnpage.speedloggernew.components

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.yml.charts.axis.AxisData
import co.yml.charts.common.model.Point
import co.yml.charts.ui.linechart.LineChart
import co.yml.charts.ui.linechart.model.GridLines
import co.yml.charts.ui.linechart.model.IntersectionPoint
import co.yml.charts.ui.linechart.model.Line
import co.yml.charts.ui.linechart.model.LineChartData
import co.yml.charts.ui.linechart.model.LinePlotData
import co.yml.charts.ui.linechart.model.LineStyle
import co.yml.charts.ui.linechart.model.SelectionHighlightPoint
import co.yml.charts.ui.linechart.model.SelectionHighlightPopUp
import co.yml.charts.ui.linechart.model.ShadowUnderLine
import com.hnpage.speedloggernew.db.LocationViewModel
import com.hnpage.speedloggernew.format
import com.hnpage.speedloggernew.global.DataInterface
import com.hnpage.speedloggernew.utils.GlobalFunction
import ir.ehsannarmani.compose_charts.models.DrawStyle
import ir.ehsannarmani.compose_charts.models.GridProperties
import ir.ehsannarmani.compose_charts.models.LabelHelperProperties
import ir.ehsannarmani.compose_charts.models.LineProperties
import ir.ehsannarmani.compose_charts.models.StrokeStyle
import kotlinx.coroutines.delay

class UIComponents {
    @Composable
    fun ChartLine3(lctViewModel: LocationViewModel) {
        val dataList = lctViewModel.getAllSpeedRecords().collectAsState(initial = emptyList())
        //Log.d("chartline3", dataList.value.toString())
        //convert speedData to list of Point
        val pointsData: List<Point> =
            dataList.value.take(20).map { Point(it.id.toFloat(), it.speed.toFloat()) }
        //Log.d("datalist point",pointsData.toString())
        val maxSpeed = dataList.value.take(20).maxOfOrNull { it.speed.toInt() }?.plus(1)
        val steps = 5
        //Log.d("steps", steps.toString())
        //get screen width in dp
        val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp

        val xAxisData = AxisData.Builder().bottomPadding(100.dp).axisStepSize(screenWidthDp / 25)
            .backgroundColor(Color.White).steps(pointsData.size - 1).labelData { i ->
                GlobalFunction().convertTimestampToDateTime(dataList.value[i].timeStamp.toLong())
            }.axisLabelAngle(90f).axisLabelColor(Color.Black).axisLineColor(Color.Black)
            .axisLabelFontSize(5.sp).shouldDrawAxisLineTillEnd(flag = true)
            .labelAndAxisLinePadding(1.dp).build()

        val yAxisData = steps.let {
            AxisData.Builder().steps(it).backgroundColor(Color.White).labelAndAxisLinePadding(10.dp)
                .axisLabelFontSize(10.sp).labelData { i ->
                    val yScale = maxSpeed?.div(steps)
                    (i * yScale!!).toString()
                }.build()
        }

        val lineChartData = LineChartData(
            linePlotData = LinePlotData(
                lines = listOf(
                    Line(
                        dataPoints = pointsData,
                        LineStyle(color = Color.Green),
                        IntersectionPoint(radius = 1.dp),
                        SelectionHighlightPoint(),
                        ShadowUnderLine(),
                        SelectionHighlightPopUp()
                    )
                ),
            ),
            xAxisData = xAxisData,
            yAxisData = yAxisData,
            gridLines = GridLines(),
            backgroundColor = Color.White,
            isZoomAllowed = true
        )

        if (pointsData.isNotEmpty()) LineChart(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp), lineChartData = lineChartData
        )
    }

    val axisProperties = GridProperties.AxisProperties(
        enabled = true,
        style = StrokeStyle.Dashed(intervals = floatArrayOf(10f, 10f)),
        color = SolidColor(Color.Gray),
        thickness = (.5).dp,
        lineCount = 5
    )

    val lineProperties = LineProperties(
        enabled = true,
        style = StrokeStyle.Dashed(intervals = floatArrayOf(10f, 10f)),
        color = SolidColor(Color.Gray),
        thickness = (.5).dp,
    )
    val labelHelperProperties = LabelHelperProperties(
        enabled = true, textStyle = TextStyle.Default.copy(fontSize = 10.sp)
    )


    @Composable
    fun SpeedChartNew(data: List<Double>) {
        //Log.d("SpeedChartNewData", data.toString())

        ir.ehsannarmani.compose_charts.LineChart(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp), data = listOf(
                ir.ehsannarmani.compose_charts.models.Line(
                    label = "Speed",
                    values = data,
                    color = SolidColor(Color(0xFF23af92)),
                    firstGradientFillColor = Color(0xFF2BC0A1).copy(alpha = .5f),
                    secondGradientFillColor = Color.Transparent,
                    //strokeAnimationSpec = tween(2000, easing = EaseInOutCubic),
                    //gradientAnimationDelay = 1000,
                    drawStyle = DrawStyle.Stroke(width = 2.dp),
                )
            )/* , animationMode = AnimationMode.Together(delayBuilder = {
             it * 500L
         })*/, labelHelperProperties = labelHelperProperties
        )
    }


    @Composable
    fun ChartLine2(speedData: List<Pair<Float, Float>>) {
        //convert speedData to list of Point
        val pointsData: List<Point> = speedData.map { Point(it.first, it.second) }.takeLast(10)
        //Log.d("datalist point",pointsData.toString())
        val maxSpeed = speedData.maxOfOrNull { it.second.toInt() }?.plus(1)
        val steps = 5
        //Log.d("steps", steps.toString())
        //get screen width in dp
        val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp

        val xAxisData = AxisData.Builder().axisStepSize(screenWidthDp / 10).backgroundColor(Color.White)
            .steps(pointsData.size - 1).labelData { i -> i.toString() }.labelAndAxisLinePadding(15.dp)
            .axisLabelAngle(90f).axisLabelColor(Color.Black).axisLineColor(Color.Black)
            .axisLabelFontSize(10.sp).shouldDrawAxisLineTillEnd(flag = true).build()

        val yAxisData = steps.let {
            AxisData.Builder().steps(it).backgroundColor(Color.White).labelAndAxisLinePadding(20.dp)
                .labelData { i ->
                    val yScale = maxSpeed?.div(steps)
                    (i * yScale!!).toString()
                }.build()
        }

        val lineChartData = LineChartData(
            linePlotData = LinePlotData(
                lines = listOf(
                    Line(
                        dataPoints = pointsData,
                        LineStyle(color = Color.Green),
                        IntersectionPoint(radius = 1.dp),
                        SelectionHighlightPoint(),
                        ShadowUnderLine(),
                        SelectionHighlightPopUp()
                    )
                ),
            ),
            xAxisData = xAxisData,
            yAxisData = yAxisData,
            gridLines = GridLines(),
            backgroundColor = Color.White,
            isZoomAllowed = true
        )

        if (pointsData.isNotEmpty()) LineChart(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp), lineChartData = lineChartData
        )
    }

    @Composable
    fun SpeedChartScreen(context: Context) {
        val speedHistory = remember { mutableStateOf(emptyList<Pair<Float, Float>>()) }

        LaunchedEffect(Unit) {
            while (true) {
                speedHistory.value = GlobalFunction().readSpeedDataFromExcel(context)
                delay(1000)
            }

        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val maxSpeed = speedHistory.value.maxOfOrNull { it.second } ?: 100f
                val scaleX = size.width / (speedHistory.value.size + 1)
                val scaleY = size.height / maxSpeed

                val path = Path().apply {
                    moveTo(0f, size.height - (speedHistory.value.firstOrNull()?.second ?: 0f) * scaleY)
                    speedHistory.value.forEach { (time, speed) ->
                        lineTo(time * scaleX, size.height - (speed * scaleY))
                    }
                }

                drawPath(path, Color.Blue, style = Stroke(5f))
            }
        }
    }

    @Composable
    fun LocationList(lctViewModel: LocationViewModel) {
        val dataList = lctViewModel.getAllSpeedRecords().collectAsState(initial = emptyList())
        val startTime: Long = GlobalFunction().stringToTimestamp("2025-02-02 00:00:00")
        val endTime: Long = GlobalFunction().stringToTimestamp("2025-02-02 23:59:59")
        //convert string datetime to long timestamp


        //val dataList = lctViewModel.getSpeedRecordsInRange(startTime, endTime).collectAsState(initial = emptyList())
        //Log.d("LocationList", dataList.value.toString())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Speed Data History",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray,
                fontStyle = FontStyle.Italic
            )

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(dataList.value) { item ->
                    LocationItem(item)
                }

            }
        }
    }

    @Composable
    fun LocationItem(speedData: DataInterface.LocationData2) {
        val formattedDate = GlobalFunction().convertTimestampToDateTime(speedData.timeStamp.toLong())

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(2.dp)
                .background(Color.White),
            elevation = CardDefaults.cardElevation()
        ) {
            Column(
                modifier = Modifier.padding(all = 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceAround, modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "ID: ${speedData.id}", color = Color.Gray, fontSize = 12.sp)
                    Text(text = formattedDate, color = Color.Gray, fontSize = 12.sp)
                    Text(
                        text = "${(speedData.speed).format(1)} km/h",
                        fontWeight = FontWeight.Bold,
                        color = Color.Blue,
                        fontSize = 12.sp
                    )/* Text(text = "Lat: ${speedData.latitude}", color = Color.Blue, fontSize = 12.sp)
                 Text(text = "Lon: ${speedData.longitude}", color = Color.Blue, fontSize = 12.sp)*/
                }
                Row(
                    horizontalArrangement = Arrangement.SpaceAround, modifier = Modifier.fillMaxWidth()
                ) {


                }


            }
        }
    }

}