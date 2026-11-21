package com.bustrack.driver

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

val NeonGreen = Color(0xFF39FF14)
val NeonBlue = Color(0xFF00FFFF)
val DarkGray = Color(0xFF1E1E1E)
val AlertRed = Color(0xFFFF003C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverDashboard() {
    val context = LocalContext.current

    var routeId by remember { mutableStateOf("") }
    var busId by remember { mutableStateOf("") }
    var isBroadcasting by remember { mutableStateOf(false) }

    // Status: 1 = Active, 2 = Delayed, 3 = Breakdown
    var statusCode by remember { mutableIntStateOf(1) }
    var delayTime by remember { mutableLongStateOf(0L) }

    val canStart = routeId.isNotBlank() && busId.isNotBlank()

    // Effect to update service when status changes while broadcasting
    LaunchedEffect(statusCode, delayTime) {
        if (isBroadcasting) {
            updateService(context, routeId, busId, statusCode, delayTime, false)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "BUS BROADCASTER",
            color = NeonBlue,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 32.dp, bottom = 48.dp)
        )

        // Setup Card
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkGray),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                OutlinedTextField(
                    value = routeId,
                    onValueChange = { if (!isBroadcasting) routeId = it },
                    label = { Text("Route Number", color = Color.LightGray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonBlue,
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = busId,
                    onValueChange = { if (!isBroadcasting) busId = it },
                    label = { Text("Bus ID", color = Color.LightGray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonBlue,
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Main Action
        Button(
            onClick = {
                if (!isBroadcasting) {
                    isBroadcasting = true
                    statusCode = 1
                    delayTime = 0L
                    updateService(context, routeId, busId, statusCode, delayTime, false)
                } else {
                    isBroadcasting = false
                    updateService(context, routeId, busId, statusCode, delayTime, true)
                }
            },
            enabled = canStart,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isBroadcasting) AlertRed else NeonGreen,
                disabledContainerColor = Color.DarkGray
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            Text(
                text = if (isBroadcasting) "STOP BROADCAST" else "START BROADCAST",
                color = if (canStart) Color.Black else Color.Gray,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (isBroadcasting) {
            Spacer(modifier = Modifier.height(48.dp))
            
            // Massive Active Toggle
            Button(
                onClick = {
                    statusCode = 1
                    delayTime = 0L
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (statusCode == 1) NeonGreen else DarkGray
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            ) {
                Text(
                    text = "ACTIVE",
                    color = if (statusCode == 1) Color.Black else Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Delayed Button
            Button(
                onClick = {
                    val calendar = Calendar.getInstance()
                    TimePickerDialog(
                        context,
                        { _, hourOfDay, minute ->
                            val targetCal = Calendar.getInstance()
                            targetCal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                            targetCal.set(Calendar.MINUTE, minute)
                            targetCal.set(Calendar.SECOND, 0)
                            
                            // If selected time is earlier than now, assume tomorrow
                            if (targetCal.before(Calendar.getInstance())) {
                                targetCal.add(Calendar.DAY_OF_YEAR, 1)
                            }
                            
                            statusCode = 2
                            delayTime = targetCal.timeInMillis / 1000L
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        true
                    ).show()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (statusCode == 2) Color(0xFFFFA500) else DarkGray
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                Text(
                    text = if (statusCode == 2) "DELAYED" else "MARK DELAYED",
                    color = if (statusCode == 2) Color.Black else Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Breakdown Secondary Button
            TextButton(
                onClick = {
                    statusCode = 3
                    delayTime = 0L
                },
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text(
                    text = "REPORT BREAKDOWN",
                    color = if (statusCode == 3) AlertRed else Color.Gray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

private fun updateService(
    context: Context,
    routeId: String,
    busId: String,
    statusCode: Int,
    delayTime: Long,
    stopService: Boolean
) {
    val intent = Intent(context, LocationTrackingService::class.java).apply {
        putExtra("EXTRA_ROUTE_ID", routeId)
        putExtra("EXTRA_BUS_ID", busId)
        putExtra("EXTRA_STATUS_CODE", statusCode)
        putExtra("EXTRA_DELAY_TIME", delayTime)
        putExtra("EXTRA_STOP_SERVICE", stopService)
    }
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && !stopService) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}
