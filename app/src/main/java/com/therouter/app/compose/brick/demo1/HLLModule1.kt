package com.therouter.app.compose.brick.demo1

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.therouter.app.compose.PathIndex.TEST_COMPOSE1
import com.therouter.app.R
import com.therouter.brick.annotation.DataProvider
import com.therouter.router.Navigator
import com.therouter.router.Route
import kotlinx.coroutines.launch


// 车辆数据类
data class VehicleDimensions(
    val width: String,
    val length: String,
    val height: String,
)

data class VehicleInfo(
    val name: String,
    val description: String,
    val capacity: String,
    val dimensions: VehicleDimensions,
)

@DataProvider(path = TEST_COMPOSE1)
fun makeData1(navigator: Navigator): List<VehicleInfo> {
    return listOf(
        VehicleInfo(
            name = "小面",
            description = "小面包车",
            capacity = "可装1-2人物品,及1-2件家具家电",
            dimensions = VehicleDimensions("1.3米", "1.8米", "1.1米"),
        ),
        VehicleInfo(
            name = "中面",
            description = "中面包车",
            capacity = "可装2-3人物品,及2-3件家具家电",
            dimensions = VehicleDimensions("1.5米", "2.0米", "1.3米"),
        ),
        VehicleInfo(
            name = "依维柯",
            description = "依维柯",
            capacity = "可装3-4人物品,及3-4件家具家电",
            dimensions = VehicleDimensions("1.8米", "2.5米", "1.5米"),
        ),
        VehicleInfo(
            name = "中货",
            description = "中型货车",
            capacity = "可装4-5人物品,及4-5件家具家电",
            dimensions = VehicleDimensions("2.0米", "3.0米", "1.8米"),
        ),
    )
}

/**
 * 测试优先级更高的数据源
 */
@DataProvider(path = TEST_COMPOSE1, priority = 5)
fun makeData2(navigator: Navigator): List<VehicleInfo> {
    return listOf(
        VehicleInfo(
            name = "小面2",
            description = "小面包车2",
            capacity = "可装1-2人物品,及1-2件家具家电",
            dimensions = VehicleDimensions("1.3米", "1.8米", "1.1米"),
        ),
        VehicleInfo(
            name = "中面2",
            description = "中面包车2",
            capacity = "可装2-3人物品,及2-3件家具家电",
            dimensions = VehicleDimensions("1.5米", "2.0米", "1.3米"),
        ),
        VehicleInfo(
            name = "依维柯2",
            description = "依维柯2",
            capacity = "可装3-4人物品,及3-4件家具家电",
            dimensions = VehicleDimensions("1.8米", "2.5米", "1.5米"),
        ),
        VehicleInfo(
            name = "中货2",
            description = "中型货车2",
            capacity = "可装4-5人物品,及4-5件家具家电",
            dimensions = VehicleDimensions("2.0米", "3.0米", "1.8米"),
        ),
    )
}

@Route(path = TEST_COMPOSE1)
@Composable
fun VehicleSelectorSection(vehicles: List<VehicleInfo>) {
    val pagerState = rememberPagerState(pageCount = { vehicles.size })
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .background(
                color = Color(0xFFFFF5F0),
                shape = RoundedCornerShape(12.dp),
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 顶部标签栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            vehicles.forEachIndexed { index, vehicle ->
                val isSelected = pagerState.currentPage == index
                Surface(
                    modifier = Modifier.clickable {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) {
                        Color(0xFFFF6B35)
                    } else {
                        Color.Transparent
                    },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = vehicle.name,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = if (isSelected) Color.White else Color.Black,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            ),
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }

        // 车型内容区域（可横滑）
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            val vehicle = vehicles[page]
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // 车辆图片区域
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF5F5F5)),
                    contentAlignment = Alignment.Center,
                ) {
                    // 车辆图片
                    Image(
                        painter = painterResource(R.drawable.main_no_vehicle_default),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentScale = ContentScale.Fit,
                    )

                    // 尺寸标注
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.BottomStart,
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = "${vehicle.dimensions.length} × ${vehicle.dimensions.width} × ${vehicle.dimensions.height}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.Black,
                                    fontSize = 12.sp,
                                ),
                                modifier = Modifier
                                    .background(
                                        Color.White.copy(alpha = 0.8f),
                                        RoundedCornerShape(4.dp),
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }
                }

                // 描述文字
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${vehicle.description} | ${vehicle.capacity}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    Row(
                        modifier = Modifier
                            .clickable { /* 查看详情 */ }
                            .padding(start = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "查看详情",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFFF6B35),
                                textDecoration = TextDecoration.Underline,
                                fontSize = 12.sp,
                            ),
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFFFF6B35),
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }

        // 底部按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 仅需用车按钮
            Button(
                onClick = { /* 仅需用车 */ },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFE0D6),
                ),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    text = "仅需用车",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFFFF6B35),
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }

            // 需要搬运按钮（带优惠标签）
            Box(
                modifier = Modifier.weight(1f),
            ) {
                Button(
                    onClick = { /* 需要搬运 */ },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF6B35),
                    ),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        text = "需要搬运",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }

                // 优惠标签
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 8.dp, y = (-8).dp),
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFFFD700),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = Color(0xFFFF6B35),
                            modifier = Modifier.size(10.dp),
                        )
                        Text(
                            text = "全程搬运·立减10元",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFFF6B35),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                }
            }
        }
    }
}
