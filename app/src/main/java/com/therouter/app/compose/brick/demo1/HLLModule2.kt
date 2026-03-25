package com.therouter.app.compose.brick.demo1

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Weekend
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.therouter.app.compose.PathIndex.TEST_COMPOSE2
import com.therouter.brick.annotation.DataProvider
import com.therouter.router.Navigator
import com.therouter.router.Route
import kotlinx.coroutines.delay

data class ServiceItemData(
    val icon: ImageVector,
    val title: String,
    val priceTag: String?,
)

/**
 * 使用 Compose 推荐的 LoadingState 模式处理网络加载状态
 */
sealed class LoadingState<out T> {
    object Loading : LoadingState<Nothing>()
    data class Success<T>(val data: T) : LoadingState<T>()
    data class Error(val message: String) : LoadingState<Nothing>()
}

@DataProvider(path = TEST_COMPOSE2)
fun makeCompose2Data(navigator: Navigator): suspend () -> List<ServiceItemData> {
    // TheRouterCompose 中允许定义返回值为 suspend 函数，可以做到数据延迟加载
    return suspend {
        delay(2000) // 模拟网络请求延迟 2 秒
        listOf(
            ServiceItemData(
                icon = Icons.Outlined.Weekend,
                title = "仅搬大件",
                priceTag = null,
            ),
            ServiceItemData(
                icon = Icons.Outlined.Build,
                title = "安装拆卸",
                priceTag = "34元起",
            ),
            ServiceItemData(
                icon = Icons.Outlined.CleaningServices,
                title = "保洁清洗",
                priceTag = null,
            ),
            ServiceItemData(
                icon = Icons.Outlined.Inventory2,
                title = "跨城搬家",
                priceTag = null,
            ),
        )
    }
}

@Route(path = TEST_COMPOSE2)
@Composable
fun MoreServicesSection(list: suspend () -> List<ServiceItemData>) {
    var loadingState by remember { mutableStateOf<LoadingState<List<ServiceItemData>>>(LoadingState.Loading) }

    LaunchedEffect(Unit) {
        try {
            val data = list() // 调用 suspend 函数获取数据
            loadingState = LoadingState.Success(data)
        } catch (e: Exception) {
            loadingState = LoadingState.Error(e.message ?: "加载失败")
        }
    }

    when (val state = loadingState) {
        is LoadingState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        is LoadingState.Success -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .background(
                        color = Color.White,
                        shape = RoundedCornerShape(12.dp),
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "更多服务",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    state.data.forEach {
                        ServiceItem(it.icon, it.title, it.priceTag)
                    }
                }
            }
        }
        is LoadingState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "加载失败: ${state.message}",
                    color = Color.Red,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun ServiceItem(
    icon: ImageVector,
    title: String,
    priceTag: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            contentAlignment = Alignment.TopEnd,
        ) {
            // 图标背景容器
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = Color(0xFFF5F5F5),
                        shape = RoundedCornerShape(8.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(32.dp),
                    tint = Color(0xFF666666),
                )
            }

            // 价格标签（如果有）
            priceTag?.let { tag ->
                Surface(
                    modifier = Modifier
                        .offset(x = 4.dp, y = (-4).dp),
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFFF6B35),
                ) {
                    Text(
                        text = tag,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }
        }

        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.sp,
                color = Color(0xFF333333),
            ),
        )
    }
}
