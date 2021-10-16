/* USER CODE BEGIN Header */
/**
  ******************************************************************************
  * @file           : main.c
  * @brief          : Main program body
  ******************************************************************************
  * @attention
  *
  * <h2><center>&copy; Copyright (c) 2021 STMicroelectronics.
  * All rights reserved.</center></h2>
  *
  * This software component is licensed by ST under BSD 3-Clause license,
  * the "License"; You may not use this file except in compliance with the
  * License. You may obtain a copy of the License at:
  *                        opensource.org/licenses/BSD-3-Clause
  *
  ******************************************************************************
  */
/* USER CODE END Header */
/* Includes ------------------------------------------------------------------*/
#include "main.h"
#include "cmsis_os.h"

/* Private includes ----------------------------------------------------------*/
/* USER CODE BEGIN Includes */
#include "oled.h"
/* USER CODE END Includes */

/* Private typedef -----------------------------------------------------------*/
/* USER CODE BEGIN PTD */

/* USER CODE END PTD */

/* Private define ------------------------------------------------------------*/
/* USER CODE BEGIN PD */
/* USER CODE END PD */

/* Private macro -------------------------------------------------------------*/
/* USER CODE BEGIN PM */

/* USER CODE END PM */

/* Private variables ---------------------------------------------------------*/
TIM_HandleTypeDef htim1;
TIM_HandleTypeDef htim2;
TIM_HandleTypeDef htim3;
TIM_HandleTypeDef htim4;
TIM_HandleTypeDef htim8;

UART_HandleTypeDef huart3;

/* Definitions for defaultTask */
osThreadId_t defaultTaskHandle;
const osThreadAttr_t defaultTask_attributes = {
  .name = "defaultTask",
  .stack_size = 128 * 4,
  .priority = (osPriority_t) osPriorityNormal,
};
/* Definitions for ShowTask */
osThreadId_t ShowTaskHandle;
const osThreadAttr_t ShowTask_attributes = {
  .name = "ShowTask",
  .stack_size = 128 * 4,
  .priority = (osPriority_t) osPriorityLow,
};
/* Definitions for MotorTask */
osThreadId_t MotorTaskHandle;
const osThreadAttr_t MotorTask_attributes = {
  .name = "MotorTask",
  .stack_size = 128 * 4,
  .priority = (osPriority_t) osPriorityLow,
};
/* Definitions for EncoderTask */
osThreadId_t EncoderTaskHandle;
const osThreadAttr_t EncoderTask_attributes = {
  .name = "EncoderTask",
  .stack_size = 128 * 4,
  .priority = (osPriority_t) osPriorityLow,
};
/* USER CODE BEGIN PV */

/* USER CODE END PV */

/* Private function prototypes -----------------------------------------------*/
void SystemClock_Config(void);
static void MX_GPIO_Init(void);
static void MX_TIM8_Init(void);
static void MX_TIM2_Init(void);
static void MX_TIM1_Init(void);
static void MX_USART3_UART_Init(void);
static void MX_TIM3_Init(void);
static void MX_TIM4_Init(void);
void StartDefaultTask(void *argument);
void show(void *argument);
void motors(void *argument);
void encoder_task(void *argument);

/* USER CODE BEGIN PFP */

/* USER CODE END PFP */

/* Private user code ---------------------------------------------------------*/
/* USER CODE BEGIN 0 */

uint8_t aRxBuffer[100];
uint8_t aTxBuffer[1];
void delay(uint16_t time)
{
	__HAL_TIM_SET_COUNTER(&htim4, 0);
	while(__HAL_TIM_GET_COUNTER(&htim4) < time);
}

uint32_t IC_Val1 = 0;
uint32_t IC_Val2 = 0;
uint32_t Difference = 0;
uint8_t Is_First_Captured = 0;  // is the first value captured ?
uint8_t Distance  = 0;

#define TRIG_PIN GPIO_PIN_13
#define TRIG_PORT GPIOD

void HAL_TIM_IC_CaptureCallback(TIM_HandleTypeDef *htim)
{
	if (htim->Channel == HAL_TIM_ACTIVE_CHANNEL_1)  // if the interrupt source is channel1
	{
		if (Is_First_Captured==0) // if the first value is not captured
		{
			IC_Val1 = HAL_TIM_ReadCapturedValue(htim, TIM_CHANNEL_1); // read the first value
			Is_First_Captured = 1;  // set the first captured as true
			// Now change the polarity to falling edge
			__HAL_TIM_SET_CAPTUREPOLARITY(htim, TIM_CHANNEL_1, TIM_INPUTCHANNELPOLARITY_FALLING);
		}

		else if (Is_First_Captured==1)   // if the first is already captured
		{
			IC_Val2 = HAL_TIM_ReadCapturedValue(htim, TIM_CHANNEL_1);  // read second value
			__HAL_TIM_SET_COUNTER(htim, 0);  // reset the counter

			if (IC_Val2 > IC_Val1)
			{
				Difference = IC_Val2-IC_Val1;
			}

			else if (IC_Val1 > IC_Val2)
			{
				Difference = (0xffff - IC_Val1) + IC_Val2;
			}

			Distance = Difference * .034/2;
			Is_First_Captured = 0; // set it back to false

			// set polarity to rising edge
			uint8_t hello[20];
			sprintf(hello,"dist: %5d\0", Distance);
			OLED_ShowString(0,30,hello);
			OLED_Refresh_Gram();
			__HAL_TIM_SET_CAPTUREPOLARITY(htim, TIM_CHANNEL_1, TIM_INPUTCHANNELPOLARITY_RISING);
			__HAL_TIM_DISABLE_IT(&htim4, TIM_IT_CC1);
		}
	}
}

void HCSR04_Read (void)
{
	HAL_GPIO_WritePin(TRIG_PORT, TRIG_PIN, GPIO_PIN_SET);  // pull the TRIG pin HIGH
	delay(10);  // wait for 10 us
	HAL_GPIO_WritePin(TRIG_PORT, TRIG_PIN, GPIO_PIN_RESET);  // pull the TRIG pin low

	__HAL_TIM_ENABLE_IT(&htim4, TIM_IT_CC1);
}
/* USER CODE END 0 */

/**
  * @brief  The application entry point.
  * @retval int
  */
int main(void)
{
  /* USER CODE BEGIN 1 */

  /* USER CODE END 1 */

  /* MCU Configuration--------------------------------------------------------*/

  /* Reset of all peripherals, Initializes the Flash interface and the Systick. */
  HAL_Init();

  /* USER CODE BEGIN Init */

  /* USER CODE END Init */

  /* Configure the system clock */
  SystemClock_Config();

  /* USER CODE BEGIN SysInit */

  /* USER CODE END SysInit */

  /* Initialize all configured peripherals */
  MX_GPIO_Init();
  MX_TIM8_Init();
  MX_TIM2_Init();
  MX_TIM1_Init();
  MX_USART3_UART_Init();
  MX_TIM3_Init();
  MX_TIM4_Init();
  /* USER CODE BEGIN 2 */

  OLED_Init();
  HAL_UART_Receive_IT(&huart3, (uint8_t *) aRxBuffer, 100);
  HAL_UART_Transmit_IT(&huart3, (uint8_t *) aTxBuffer, 1);
  HAL_TIM_IC_Start_IT(&htim4, TIM_CHANNEL_1);
  /* USER CODE END 2 */

  /* Init scheduler */
  osKernelInitialize();

  /* USER CODE BEGIN RTOS_MUTEX */
  /* add mutexes, ... */
  /* USER CODE END RTOS_MUTEX */

  /* USER CODE BEGIN RTOS_SEMAPHORES */
  /* add semaphores, ... */
  /* USER CODE END RTOS_SEMAPHORES */

  /* USER CODE BEGIN RTOS_TIMERS */
  /* start timers, add new ones, ... */
  /* USER CODE END RTOS_TIMERS */

  /* USER CODE BEGIN RTOS_QUEUES */
  /* add queues, ... */
  /* USER CODE END RTOS_QUEUES */

  /* Create the thread(s) */
  /* creation of defaultTask */
  defaultTaskHandle = osThreadNew(StartDefaultTask, NULL, &defaultTask_attributes);

  /* creation of ShowTask */
  ShowTaskHandle = osThreadNew(show, NULL, &ShowTask_attributes);

  /* creation of MotorTask */
  MotorTaskHandle = osThreadNew(motors, NULL, &MotorTask_attributes);

  /* creation of EncoderTask */
  EncoderTaskHandle = osThreadNew(encoder_task, NULL, &EncoderTask_attributes);

  /* USER CODE BEGIN RTOS_THREADS */
  /* add threads, ... */
  /* USER CODE END RTOS_THREADS */

  /* USER CODE BEGIN RTOS_EVENTS */
  /* add events, ... */
  /* USER CODE END RTOS_EVENTS */

  /* Start scheduler */
  osKernelStart();

  /* We should never get here as control is now taken by the scheduler */
  /* Infinite loop */
  /* USER CODE BEGIN WHILE */
  while (1)
  {
    /* USER CODE END WHILE */

    /* USER CODE BEGIN 3 */
  }
  /* USER CODE END 3 */
}

/**
  * @brief System Clock Configuration
  * @retval None
  */
void SystemClock_Config(void)
{
  RCC_OscInitTypeDef RCC_OscInitStruct = {0};
  RCC_ClkInitTypeDef RCC_ClkInitStruct = {0};

  /** Initializes the RCC Oscillators according to the specified parameters
  * in the RCC_OscInitTypeDef structure.
  */
  RCC_OscInitStruct.OscillatorType = RCC_OSCILLATORTYPE_HSI;
  RCC_OscInitStruct.HSIState = RCC_HSI_ON;
  RCC_OscInitStruct.HSICalibrationValue = RCC_HSICALIBRATION_DEFAULT;
  RCC_OscInitStruct.PLL.PLLState = RCC_PLL_NONE;
  if (HAL_RCC_OscConfig(&RCC_OscInitStruct) != HAL_OK)
  {
    Error_Handler();
  }
  /** Initializes the CPU, AHB and APB buses clocks
  */
  RCC_ClkInitStruct.ClockType = RCC_CLOCKTYPE_HCLK|RCC_CLOCKTYPE_SYSCLK
                              |RCC_CLOCKTYPE_PCLK1|RCC_CLOCKTYPE_PCLK2;
  RCC_ClkInitStruct.SYSCLKSource = RCC_SYSCLKSOURCE_HSI;
  RCC_ClkInitStruct.AHBCLKDivider = RCC_SYSCLK_DIV1;
  RCC_ClkInitStruct.APB1CLKDivider = RCC_HCLK_DIV1;
  RCC_ClkInitStruct.APB2CLKDivider = RCC_HCLK_DIV1;

  if (HAL_RCC_ClockConfig(&RCC_ClkInitStruct, FLASH_LATENCY_0) != HAL_OK)
  {
    Error_Handler();
  }
}

/**
  * @brief TIM1 Initialization Function
  * @param None
  * @retval None
  */
static void MX_TIM1_Init(void)
{

  /* USER CODE BEGIN TIM1_Init 0 */

  /* USER CODE END TIM1_Init 0 */

  TIM_ClockConfigTypeDef sClockSourceConfig = {0};
  TIM_MasterConfigTypeDef sMasterConfig = {0};
  TIM_OC_InitTypeDef sConfigOC = {0};
  TIM_BreakDeadTimeConfigTypeDef sBreakDeadTimeConfig = {0};

  /* USER CODE BEGIN TIM1_Init 1 */

  /* USER CODE END TIM1_Init 1 */
  htim1.Instance = TIM1;
  htim1.Init.Prescaler = 160;
  htim1.Init.CounterMode = TIM_COUNTERMODE_UP;
  htim1.Init.Period = 1000;
  htim1.Init.ClockDivision = TIM_CLOCKDIVISION_DIV1;
  htim1.Init.RepetitionCounter = 0;
  htim1.Init.AutoReloadPreload = TIM_AUTORELOAD_PRELOAD_ENABLE;
  if (HAL_TIM_Base_Init(&htim1) != HAL_OK)
  {
    Error_Handler();
  }
  sClockSourceConfig.ClockSource = TIM_CLOCKSOURCE_INTERNAL;
  if (HAL_TIM_ConfigClockSource(&htim1, &sClockSourceConfig) != HAL_OK)
  {
    Error_Handler();
  }
  if (HAL_TIM_PWM_Init(&htim1) != HAL_OK)
  {
    Error_Handler();
  }
  sMasterConfig.MasterOutputTrigger = TIM_TRGO_RESET;
  sMasterConfig.MasterSlaveMode = TIM_MASTERSLAVEMODE_DISABLE;
  if (HAL_TIMEx_MasterConfigSynchronization(&htim1, &sMasterConfig) != HAL_OK)
  {
    Error_Handler();
  }
  sConfigOC.OCMode = TIM_OCMODE_PWM1;
  sConfigOC.Pulse = 0;
  sConfigOC.OCPolarity = TIM_OCPOLARITY_HIGH;
  sConfigOC.OCFastMode = TIM_OCFAST_DISABLE;
  sConfigOC.OCIdleState = TIM_OCIDLESTATE_RESET;
  sConfigOC.OCNIdleState = TIM_OCNIDLESTATE_RESET;
  if (HAL_TIM_PWM_ConfigChannel(&htim1, &sConfigOC, TIM_CHANNEL_4) != HAL_OK)
  {
    Error_Handler();
  }
  sBreakDeadTimeConfig.OffStateRunMode = TIM_OSSR_DISABLE;
  sBreakDeadTimeConfig.OffStateIDLEMode = TIM_OSSI_DISABLE;
  sBreakDeadTimeConfig.LockLevel = TIM_LOCKLEVEL_OFF;
  sBreakDeadTimeConfig.DeadTime = 0;
  sBreakDeadTimeConfig.BreakState = TIM_BREAK_DISABLE;
  sBreakDeadTimeConfig.BreakPolarity = TIM_BREAKPOLARITY_HIGH;
  sBreakDeadTimeConfig.AutomaticOutput = TIM_AUTOMATICOUTPUT_DISABLE;
  if (HAL_TIMEx_ConfigBreakDeadTime(&htim1, &sBreakDeadTimeConfig) != HAL_OK)
  {
    Error_Handler();
  }
  /* USER CODE BEGIN TIM1_Init 2 */

  /* USER CODE END TIM1_Init 2 */
  HAL_TIM_MspPostInit(&htim1);

}

/**
  * @brief TIM2 Initialization Function
  * @param None
  * @retval None
  */
static void MX_TIM2_Init(void)
{

  /* USER CODE BEGIN TIM2_Init 0 */

  /* USER CODE END TIM2_Init 0 */

  TIM_Encoder_InitTypeDef sConfig = {0};
  TIM_MasterConfigTypeDef sMasterConfig = {0};

  /* USER CODE BEGIN TIM2_Init 1 */

  /* USER CODE END TIM2_Init 1 */
  htim2.Instance = TIM2;
  htim2.Init.Prescaler = 0;
  htim2.Init.CounterMode = TIM_COUNTERMODE_UP;
  htim2.Init.Period = 65535;
  htim2.Init.ClockDivision = TIM_CLOCKDIVISION_DIV1;
  htim2.Init.AutoReloadPreload = TIM_AUTORELOAD_PRELOAD_DISABLE;
  sConfig.EncoderMode = TIM_ENCODERMODE_TI12;
  sConfig.IC1Polarity = TIM_ICPOLARITY_RISING;
  sConfig.IC1Selection = TIM_ICSELECTION_DIRECTTI;
  sConfig.IC1Prescaler = TIM_ICPSC_DIV1;
  sConfig.IC1Filter = 10;
  sConfig.IC2Polarity = TIM_ICPOLARITY_RISING;
  sConfig.IC2Selection = TIM_ICSELECTION_DIRECTTI;
  sConfig.IC2Prescaler = TIM_ICPSC_DIV1;
  sConfig.IC2Filter = 10;
  if (HAL_TIM_Encoder_Init(&htim2, &sConfig) != HAL_OK)
  {
    Error_Handler();
  }
  sMasterConfig.MasterOutputTrigger = TIM_TRGO_RESET;
  sMasterConfig.MasterSlaveMode = TIM_MASTERSLAVEMODE_DISABLE;
  if (HAL_TIMEx_MasterConfigSynchronization(&htim2, &sMasterConfig) != HAL_OK)
  {
    Error_Handler();
  }
  /* USER CODE BEGIN TIM2_Init 2 */

  /* USER CODE END TIM2_Init 2 */

}

/**
  * @brief TIM3 Initialization Function
  * @param None
  * @retval None
  */
static void MX_TIM3_Init(void)
{

  /* USER CODE BEGIN TIM3_Init 0 */

  /* USER CODE END TIM3_Init 0 */

  TIM_Encoder_InitTypeDef sConfig = {0};
  TIM_MasterConfigTypeDef sMasterConfig = {0};

  /* USER CODE BEGIN TIM3_Init 1 */

  /* USER CODE END TIM3_Init 1 */
  htim3.Instance = TIM3;
  htim3.Init.Prescaler = 0;
  htim3.Init.CounterMode = TIM_COUNTERMODE_UP;
  htim3.Init.Period = 65535;
  htim3.Init.ClockDivision = TIM_CLOCKDIVISION_DIV1;
  htim3.Init.AutoReloadPreload = TIM_AUTORELOAD_PRELOAD_DISABLE;
  sConfig.EncoderMode = TIM_ENCODERMODE_TI12;
  sConfig.IC1Polarity = TIM_ICPOLARITY_RISING;
  sConfig.IC1Selection = TIM_ICSELECTION_DIRECTTI;
  sConfig.IC1Prescaler = TIM_ICPSC_DIV1;
  sConfig.IC1Filter = 10;
  sConfig.IC2Polarity = TIM_ICPOLARITY_RISING;
  sConfig.IC2Selection = TIM_ICSELECTION_DIRECTTI;
  sConfig.IC2Prescaler = TIM_ICPSC_DIV1;
  sConfig.IC2Filter = 10;
  if (HAL_TIM_Encoder_Init(&htim3, &sConfig) != HAL_OK)
  {
    Error_Handler();
  }
  sMasterConfig.MasterOutputTrigger = TIM_TRGO_RESET;
  sMasterConfig.MasterSlaveMode = TIM_MASTERSLAVEMODE_DISABLE;
  if (HAL_TIMEx_MasterConfigSynchronization(&htim3, &sMasterConfig) != HAL_OK)
  {
    Error_Handler();
  }
  /* USER CODE BEGIN TIM3_Init 2 */

  /* USER CODE END TIM3_Init 2 */

}

/**
  * @brief TIM4 Initialization Function
  * @param None
  * @retval None
  */
static void MX_TIM4_Init(void)
{

  /* USER CODE BEGIN TIM4_Init 0 */

  /* USER CODE END TIM4_Init 0 */

  TIM_MasterConfigTypeDef sMasterConfig = {0};
  TIM_IC_InitTypeDef sConfigIC = {0};

  /* USER CODE BEGIN TIM4_Init 1 */

  /* USER CODE END TIM4_Init 1 */
  htim4.Instance = TIM4;
  htim4.Init.Prescaler = 8;
  htim4.Init.CounterMode = TIM_COUNTERMODE_UP;
  htim4.Init.Period = 0xffff-1;
  htim4.Init.ClockDivision = TIM_CLOCKDIVISION_DIV1;
  htim4.Init.AutoReloadPreload = TIM_AUTORELOAD_PRELOAD_DISABLE;
  if (HAL_TIM_IC_Init(&htim4) != HAL_OK)
  {
    Error_Handler();
  }
  sMasterConfig.MasterOutputTrigger = TIM_TRGO_RESET;
  sMasterConfig.MasterSlaveMode = TIM_MASTERSLAVEMODE_DISABLE;
  if (HAL_TIMEx_MasterConfigSynchronization(&htim4, &sMasterConfig) != HAL_OK)
  {
    Error_Handler();
  }
  sConfigIC.ICPolarity = TIM_INPUTCHANNELPOLARITY_RISING;
  sConfigIC.ICSelection = TIM_ICSELECTION_DIRECTTI;
  sConfigIC.ICPrescaler = TIM_ICPSC_DIV1;
  sConfigIC.ICFilter = 0;
  if (HAL_TIM_IC_ConfigChannel(&htim4, &sConfigIC, TIM_CHANNEL_1) != HAL_OK)
  {
    Error_Handler();
  }
  /* USER CODE BEGIN TIM4_Init 2 */

  /* USER CODE END TIM4_Init 2 */

}

/**
  * @brief TIM8 Initialization Function
  * @param None
  * @retval None
  */
static void MX_TIM8_Init(void)
{

  /* USER CODE BEGIN TIM8_Init 0 */

  /* USER CODE END TIM8_Init 0 */

  TIM_ClockConfigTypeDef sClockSourceConfig = {0};
  TIM_MasterConfigTypeDef sMasterConfig = {0};
  TIM_OC_InitTypeDef sConfigOC = {0};
  TIM_BreakDeadTimeConfigTypeDef sBreakDeadTimeConfig = {0};

  /* USER CODE BEGIN TIM8_Init 1 */

  /* USER CODE END TIM8_Init 1 */
  htim8.Instance = TIM8;
  htim8.Init.Prescaler = 0;
  htim8.Init.CounterMode = TIM_COUNTERMODE_UP;
  htim8.Init.Period = 7199;
  htim8.Init.ClockDivision = TIM_CLOCKDIVISION_DIV1;
  htim8.Init.RepetitionCounter = 0;
  htim8.Init.AutoReloadPreload = TIM_AUTORELOAD_PRELOAD_DISABLE;
  if (HAL_TIM_Base_Init(&htim8) != HAL_OK)
  {
    Error_Handler();
  }
  sClockSourceConfig.ClockSource = TIM_CLOCKSOURCE_INTERNAL;
  if (HAL_TIM_ConfigClockSource(&htim8, &sClockSourceConfig) != HAL_OK)
  {
    Error_Handler();
  }
  if (HAL_TIM_PWM_Init(&htim8) != HAL_OK)
  {
    Error_Handler();
  }
  sMasterConfig.MasterOutputTrigger = TIM_TRGO_RESET;
  sMasterConfig.MasterSlaveMode = TIM_MASTERSLAVEMODE_DISABLE;
  if (HAL_TIMEx_MasterConfigSynchronization(&htim8, &sMasterConfig) != HAL_OK)
  {
    Error_Handler();
  }
  sConfigOC.OCMode = TIM_OCMODE_PWM1;
  sConfigOC.Pulse = 0;
  sConfigOC.OCPolarity = TIM_OCPOLARITY_HIGH;
  sConfigOC.OCNPolarity = TIM_OCNPOLARITY_HIGH;
  sConfigOC.OCFastMode = TIM_OCFAST_DISABLE;
  sConfigOC.OCIdleState = TIM_OCIDLESTATE_RESET;
  sConfigOC.OCNIdleState = TIM_OCNIDLESTATE_RESET;
  if (HAL_TIM_PWM_ConfigChannel(&htim8, &sConfigOC, TIM_CHANNEL_1) != HAL_OK)
  {
    Error_Handler();
  }
  if (HAL_TIM_PWM_ConfigChannel(&htim8, &sConfigOC, TIM_CHANNEL_2) != HAL_OK)
  {
    Error_Handler();
  }
  sBreakDeadTimeConfig.OffStateRunMode = TIM_OSSR_DISABLE;
  sBreakDeadTimeConfig.OffStateIDLEMode = TIM_OSSI_DISABLE;
  sBreakDeadTimeConfig.LockLevel = TIM_LOCKLEVEL_OFF;
  sBreakDeadTimeConfig.DeadTime = 0;
  sBreakDeadTimeConfig.BreakState = TIM_BREAK_DISABLE;
  sBreakDeadTimeConfig.BreakPolarity = TIM_BREAKPOLARITY_HIGH;
  sBreakDeadTimeConfig.AutomaticOutput = TIM_AUTOMATICOUTPUT_DISABLE;
  if (HAL_TIMEx_ConfigBreakDeadTime(&htim8, &sBreakDeadTimeConfig) != HAL_OK)
  {
    Error_Handler();
  }
  /* USER CODE BEGIN TIM8_Init 2 */

  /* USER CODE END TIM8_Init 2 */

}

/**
  * @brief USART3 Initialization Function
  * @param None
  * @retval None
  */
static void MX_USART3_UART_Init(void)
{

  /* USER CODE BEGIN USART3_Init 0 */

  /* USER CODE END USART3_Init 0 */

  /* USER CODE BEGIN USART3_Init 1 */

  /* USER CODE END USART3_Init 1 */
  huart3.Instance = USART3;
  huart3.Init.BaudRate = 115200;
  huart3.Init.WordLength = UART_WORDLENGTH_8B;
  huart3.Init.StopBits = UART_STOPBITS_1;
  huart3.Init.Parity = UART_PARITY_NONE;
  huart3.Init.Mode = UART_MODE_TX_RX;
  huart3.Init.HwFlowCtl = UART_HWCONTROL_NONE;
  huart3.Init.OverSampling = UART_OVERSAMPLING_16;
  if (HAL_UART_Init(&huart3) != HAL_OK)
  {
    Error_Handler();
  }
  /* USER CODE BEGIN USART3_Init 2 */

  /* USER CODE END USART3_Init 2 */

}

/**
  * @brief GPIO Initialization Function
  * @param None
  * @retval None
  */
static void MX_GPIO_Init(void)
{
  GPIO_InitTypeDef GPIO_InitStruct = {0};

  /* GPIO Ports Clock Enable */
  __HAL_RCC_GPIOE_CLK_ENABLE();
  __HAL_RCC_GPIOA_CLK_ENABLE();
  __HAL_RCC_GPIOD_CLK_ENABLE();
  __HAL_RCC_GPIOC_CLK_ENABLE();
  __HAL_RCC_GPIOB_CLK_ENABLE();

  /*Configure GPIO pin Output Level */
  HAL_GPIO_WritePin(GPIOE, OLED_SCL_Pin|OLED_SDA_Pin|OLED_RST_Pin|OLED_DC_Pin
                          |LED3_Pin, GPIO_PIN_RESET);

  /*Configure GPIO pin Output Level */
  HAL_GPIO_WritePin(GPIOA, AIN2_Pin|AIN1_Pin|BIN1_Pin|BIN2_Pin, GPIO_PIN_RESET);

  /*Configure GPIO pin Output Level */
  HAL_GPIO_WritePin(GPIOD, GPIO_PIN_13, GPIO_PIN_RESET);

  /*Configure GPIO pins : OLED_SCL_Pin OLED_SDA_Pin OLED_RST_Pin OLED_DC_Pin
                           LED3_Pin */
  GPIO_InitStruct.Pin = OLED_SCL_Pin|OLED_SDA_Pin|OLED_RST_Pin|OLED_DC_Pin
                          |LED3_Pin;
  GPIO_InitStruct.Mode = GPIO_MODE_OUTPUT_PP;
  GPIO_InitStruct.Pull = GPIO_NOPULL;
  GPIO_InitStruct.Speed = GPIO_SPEED_FREQ_LOW;
  HAL_GPIO_Init(GPIOE, &GPIO_InitStruct);

  /*Configure GPIO pins : AIN2_Pin AIN1_Pin */
  GPIO_InitStruct.Pin = AIN2_Pin|AIN1_Pin;
  GPIO_InitStruct.Mode = GPIO_MODE_OUTPUT_PP;
  GPIO_InitStruct.Pull = GPIO_NOPULL;
  GPIO_InitStruct.Speed = GPIO_SPEED_FREQ_HIGH;
  HAL_GPIO_Init(GPIOA, &GPIO_InitStruct);

  /*Configure GPIO pins : BIN1_Pin BIN2_Pin */
  GPIO_InitStruct.Pin = BIN1_Pin|BIN2_Pin;
  GPIO_InitStruct.Mode = GPIO_MODE_OUTPUT_PP;
  GPIO_InitStruct.Pull = GPIO_NOPULL;
  GPIO_InitStruct.Speed = GPIO_SPEED_FREQ_LOW;
  HAL_GPIO_Init(GPIOA, &GPIO_InitStruct);

  /*Configure GPIO pin : PD13 */
  GPIO_InitStruct.Pin = GPIO_PIN_13;
  GPIO_InitStruct.Mode = GPIO_MODE_OUTPUT_PP;
  GPIO_InitStruct.Pull = GPIO_NOPULL;
  GPIO_InitStruct.Speed = GPIO_SPEED_FREQ_LOW;
  HAL_GPIO_Init(GPIOD, &GPIO_InitStruct);

}

/* USER CODE BEGIN 4 */

void HAL_UART_RxCpltCallback(UART_HandleTypeDef *huart)
{
	//Prevent unused arguments(s) compilation warning
	UNUSED(huart);

	HAL_UART_Transmit(&huart3, (uint8_t *)aTxBuffer, 1, 0xFFFF);
	//HAL_UART_Receive(&huart3, (uint8_t *)aTxBuffer, 200, 0xFFFF);
	HAL_UART_Receive_IT(&huart3, aRxBuffer, 100);
	//HAL_UART_Transmit_IT(&huart3, aTxBuffer, 200);
}
/* USER CODE END 4 */

/* USER CODE BEGIN Header_StartDefaultTask */
/**
  * @brief  Function implementing the defaultTask thread.
  * @param  argument: Not used
  * @retval None
  */
/* USER CODE END Header_StartDefaultTask */
void StartDefaultTask(void *argument)
{
  /* USER CODE BEGIN 5 */
  /* Infinite loop */
	//uint8_t ch = 'A';
  for(;;)
  {
	  //HAL_UART_Transmit(&huart3, (uint8_t *) &ch, 1, 0xFFFF); //will remove later on
	  //if (ch < 'Z')//
		  //ch++;//
	  //else ch = 'A';//
	  HAL_GPIO_TogglePin(LED3_GPIO_Port, LED3_Pin);
    osDelay(5000); //Toggle every 5s
  }
  /* USER CODE END 5 */
}

/* USER CODE BEGIN Header_show */
/**
* @brief Function implementing the ShowTask thread.
* @param argument: Not used
* @retval None
*/
/* USER CODE END Header_show */
void show(void *argument)
{
  /* USER CODE BEGIN show */
	uint8_t hello[20] = "MDP Grp 17\0";
	uint8_t hello1[20];
  /* Infinite loop */
  for(;;)
  {
	  HCSR04_Read();
	  HAL_Delay(200);
	  OLED_ShowString(0,0,hello);
	  sprintf(hello1,"%s\0",aRxBuffer);
	  OLED_ShowString(0,40,hello1);
	  OLED_Refresh_Gram();
    //osDelay(1000);
  }
  /* USER CODE END show */
}

/* USER CODE BEGIN Header_motors */
/**
* @brief Function implementing the MotorTask thread.
* @param argument: Not used
* @retval None
*/
/* USER CODE END Header_motors */
void forward(int time)
{
//	if(Distance < 20)
//	{
//		osDelay(time);
//	}
//	else
//	{
		uint16_t pwmValA = 3000;
		uint16_t pwmValB = 2890;//change when charge or on that day
		center();
		HAL_GPIO_WritePin(GPIOA, AIN2_Pin, GPIO_PIN_SET);
		HAL_GPIO_WritePin(GPIOA, AIN1_Pin, GPIO_PIN_RESET);
		HAL_GPIO_WritePin(GPIOA, BIN2_Pin, GPIO_PIN_SET);
		HAL_GPIO_WritePin(GPIOA, BIN1_Pin, GPIO_PIN_RESET);
		__HAL_TIM_SetCompare(&htim8, TIM_CHANNEL_1, pwmValA);
		__HAL_TIM_SetCompare(&htim8, TIM_CHANNEL_2, pwmValB);
		osDelay(time);
//		float t = time / 1000;
//		float j = 2.45;//2.45;
//		while (j<=t)
//		{
//			osDelay(800);
//			htim1.Instance->CCR4 = 84;
//			osDelay(120);
//			htim1.Instance->CCR4 = 62;
//			osDelay(50);
//			htim1.Instance->CCR4 = 75;
//			j++;
//		}
//		if (t < j)
//		{
//			osDelay(time);
//		}
//	}
}
void forwards()
{
	while(Distance > 43)
	{
		uint16_t pwmValA = 4000; //4000 in lab or 3750
		uint16_t pwmValB = 3920;//change when charge or on that day or 4000
		center();
		HAL_GPIO_WritePin(GPIOA, AIN2_Pin, GPIO_PIN_SET);
		HAL_GPIO_WritePin(GPIOA, AIN1_Pin, GPIO_PIN_RESET);
		HAL_GPIO_WritePin(GPIOA, BIN2_Pin, GPIO_PIN_SET);
		HAL_GPIO_WritePin(GPIOA, BIN1_Pin, GPIO_PIN_RESET);
		__HAL_TIM_SetCompare(&htim8, TIM_CHANNEL_1, pwmValA);
		__HAL_TIM_SetCompare(&htim8, TIM_CHANNEL_2, pwmValB);
		//float t = 3000 / 1000;
		//float j = 2.45;//2.45;
		//while (1)
		//{
//			osDelay(1000);
//			htim1.Instance->CCR4 = 84;
//			osDelay(120);
//			htim1.Instance->CCR4 = 62;
//			osDelay(80);
//			htim1.Instance->CCR4 = 75;
			//osDelay(50);
			//j++;
		//}
	}
}
void reverse(int time)
{
	uint16_t pwmValA=2000;
	uint16_t pwmValB=1880;//change when charge or on that day
	HAL_GPIO_WritePin(GPIOA, AIN2_Pin, GPIO_PIN_RESET);
	HAL_GPIO_WritePin(GPIOA, AIN1_Pin, GPIO_PIN_SET);
	HAL_GPIO_WritePin(GPIOA, BIN2_Pin, GPIO_PIN_RESET);
	HAL_GPIO_WritePin(GPIOA, BIN1_Pin, GPIO_PIN_SET);
	__HAL_TIM_SetCompare(&htim8, TIM_CHANNEL_1, pwmValA);
	__HAL_TIM_SetCompare(&htim8, TIM_CHANNEL_2, pwmValB);
	float t = time / 1000;
	float j = 0.45;//2.45;
//	while (j<=t)
//	{
//		osDelay(1000);
//		htim1.Instance->CCR4 = 84;
//		osDelay(120);
//		htim1.Instance->CCR4 = 62;
//		osDelay(50);
//		htim1.Instance->CCR4 = 75;
//		j++;
//	}
	if (t < j)
	{
		osDelay(time);
	}
}
void leftmove(int time)
{
	uint16_t pwmValA=0;
	uint16_t pwmValB=4000; //3000 lab
	htim1.Instance->CCR4 = 62;    //extreme left 67
	HAL_GPIO_WritePin(GPIOA, AIN2_Pin, GPIO_PIN_RESET);
	HAL_GPIO_WritePin(GPIOA, AIN1_Pin, GPIO_PIN_SET);
	HAL_GPIO_WritePin(GPIOA, BIN2_Pin, GPIO_PIN_SET);
	HAL_GPIO_WritePin(GPIOA, BIN1_Pin, GPIO_PIN_RESET);
	__HAL_TIM_SetCompare(&htim8, TIM_CHANNEL_1, pwmValA);
	__HAL_TIM_SetCompare(&htim8, TIM_CHANNEL_2, pwmValB);
	osDelay(time);
}
void rightmove(int time)
{
	uint16_t pwmValA=4000; //3000 lab
	uint16_t pwmValB=0;
	htim1.Instance->CCR4 = 90;    //extreme right 84
	HAL_GPIO_WritePin(GPIOA, AIN2_Pin, GPIO_PIN_SET);
	HAL_GPIO_WritePin(GPIOA, AIN1_Pin, GPIO_PIN_RESET);
	HAL_GPIO_WritePin(GPIOA, BIN2_Pin, GPIO_PIN_RESET);
	HAL_GPIO_WritePin(GPIOA, BIN1_Pin, GPIO_PIN_SET);
	__HAL_TIM_SetCompare(&htim8, TIM_CHANNEL_1, pwmValA);
	__HAL_TIM_SetCompare(&htim8, TIM_CHANNEL_2, pwmValB);
	osDelay(time);

}
void Rleftmove(int time)
{
	uint16_t pwmValA=0;
	uint16_t pwmValB=3000;
	htim1.Instance->CCR4 = 62;    //extreme left 67
	HAL_GPIO_WritePin(GPIOA, AIN2_Pin, GPIO_PIN_SET);
	HAL_GPIO_WritePin(GPIOA, AIN1_Pin, GPIO_PIN_RESET);
	HAL_GPIO_WritePin(GPIOA, BIN2_Pin, GPIO_PIN_RESET);
	HAL_GPIO_WritePin(GPIOA, BIN1_Pin, GPIO_PIN_SET);
	__HAL_TIM_SetCompare(&htim8, TIM_CHANNEL_1, pwmValA);
	__HAL_TIM_SetCompare(&htim8, TIM_CHANNEL_2, pwmValB);
	osDelay(time);
}
void Rrightmove(int time)
{
	uint16_t pwmValA=3000;
	uint16_t pwmValB=0;
	htim1.Instance->CCR4 = 90;    //extreme right 84
	HAL_GPIO_WritePin(GPIOA, AIN2_Pin, GPIO_PIN_RESET);
	HAL_GPIO_WritePin(GPIOA, AIN1_Pin, GPIO_PIN_SET);
	HAL_GPIO_WritePin(GPIOA, BIN2_Pin, GPIO_PIN_SET);
	HAL_GPIO_WritePin(GPIOA, BIN1_Pin, GPIO_PIN_RESET);
	__HAL_TIM_SetCompare(&htim8, TIM_CHANNEL_1, pwmValA);
	__HAL_TIM_SetCompare(&htim8, TIM_CHANNEL_2, pwmValB);
	osDelay(time);
}
void stop(int time)
{
	htim1.Instance->CCR4 = 75;
	uint16_t pwmValA=0;
	uint16_t pwmValB=0;
	__HAL_TIM_SetCompare(&htim8, TIM_CHANNEL_1, pwmValA);
	__HAL_TIM_SetCompare(&htim8, TIM_CHANNEL_2, pwmValB);
	osDelay(time);
}
void center()
{
	htim1.Instance->CCR4 = 75; // can change to 73
}
void left()
{
	htim1.Instance->CCR4 = 62;    //extreme left
}
void right()
{
	htim1.Instance->CCR4 = 84;    //extreme right
}
//void checkdist(int var)
//{
//	if(var == 1)
//	{
//		if (Distance < 30)
//		{
//			stop();
//		}
//	}
//	else
//	{
//		reverse(100);
//		stop();
//	}
//	if(Distance < 15)
//	{
//		reverse
//	}
//}
void sendfeedback()
{
	uint8_t ch = 'k';
	HAL_UART_Transmit(&huart3, (uint8_t *) &ch, 1, 0xFFFF);

}
void motors(void *argument)
{
  /* USER CODE BEGIN motors */
	uint16_t pwmValA, pwmValB, pwmVal = 0;
	uint8_t i = 0;
	uint8_t cmd;

	uint8_t msg[20];

	HAL_TIM_PWM_Start(&htim8, TIM_CHANNEL_1); // DC Motor
	HAL_TIM_PWM_Start(&htim8, TIM_CHANNEL_2); // DC Motor
	HAL_TIM_PWM_Start(&htim1, TIM_CHANNEL_4); // SERVO MOTOR

  /* Infinite loop */
  for(;;)
  {
	  //AIN2_Pin|AIN1_Pin
	  //BIN1_Pin|BIN2_Pin
	  //clockwise
//	  while(pwmVal < 4000)
//	  {
//		  /*HAL_GPIO_WritePin(GPIOA, AIN2_Pin, GPIO_PIN_RESET);
//		  HAL_GPIO_WritePin(GPIOA, AIN1_Pin, GPIO_PIN_SET);
//		  HAL_GPIO_WritePin(GPIOA, BIN2_Pin, GPIO_PIN_RESET);
//		  HAL_GPIO_WritePin(GPIOA, BIN1_Pin, GPIO_PIN_SET);*/
//		  HAL_GPIO_WritePin(GPIOA, AIN2_Pin, GPIO_PIN_SET);
//		  HAL_GPIO_WritePin(GPIOA, AIN1_Pin, GPIO_PIN_RESET);
//		  HAL_GPIO_WritePin(GPIOA, BIN2_Pin, GPIO_PIN_SET);
//		  HAL_GPIO_WritePin(GPIOA, BIN1_Pin, GPIO_PIN_RESET);
//		  pwmVal++;
//		  //Modify the comparison value for the duty cycle
//		  __HAL_TIM_SetCompare(&htim8, TIM_CHANNEL_1, pwmVal);
//		  __HAL_TIM_SetCompare(&htim8, TIM_CHANNEL_2, pwmVal);
//		  osDelay(10);
//	  }
//	  //anticlockwise
//	  while (pwmVal > 0)
//	  {
//		  /*HAL_GPIO_WritePin(GPIOA, AIN2_Pin, GPIO_PIN_SET);
//		  HAL_GPIO_WritePin(GPIOA, AIN1_Pin, GPIO_PIN_RESET);
//		  HAL_GPIO_WritePin(GPIOA, BIN2_Pin, GPIO_PIN_SET);
//		  HAL_GPIO_WritePin(GPIOA, BIN1_Pin, GPIO_PIN_RESET);*/
//		  HAL_GPIO_WritePin(GPIOA, AIN2_Pin, GPIO_PIN_RESET);
//		  HAL_GPIO_WritePin(GPIOA, AIN1_Pin, GPIO_PIN_SET);
//		  HAL_GPIO_WritePin(GPIOA, BIN2_Pin, GPIO_PIN_RESET);
//		  HAL_GPIO_WritePin(GPIOA, BIN1_Pin, GPIO_PIN_SET);
//		  pwmVal--;
//		  //Modify the comparison value for the duty cycle
//		  __HAL_TIM_SetCompare(&htim8, TIM_CHANNEL_1, pwmVal);
//		  __HAL_TIM_SetCompare(&htim8, TIM_CHANNEL_2, pwmVal);
//		  osDelay(10);
//	  }
	  //while(1)
	  //{
		  HAL_UART_Receive_IT(&huart3, (uint8_t *) aRxBuffer, 100);
		  cmd=aRxBuffer[i];
		  //switch(aRxBuffer[0])
		  switch(cmd)
		  {
		  case 'w':{
			  pwmValA=3000;
			  pwmValB=2890;
			  htim1.Instance->CCR4 = 75;
			  HAL_GPIO_WritePin(GPIOA, AIN2_Pin, GPIO_PIN_SET);
			  HAL_GPIO_WritePin(GPIOA, AIN1_Pin, GPIO_PIN_RESET);
			  HAL_GPIO_WritePin(GPIOA, BIN2_Pin, GPIO_PIN_SET);
			  HAL_GPIO_WritePin(GPIOA, BIN1_Pin, GPIO_PIN_RESET);
			  __HAL_TIM_SetCompare(&htim8, TIM_CHANNEL_1, pwmValA);
			  __HAL_TIM_SetCompare(&htim8, TIM_CHANNEL_2, pwmValB);
//			  osDelay(5000);
//			  pwmValA=0;
//			  pwmValB=0;
//			  __HAL_TIM_SetCompare(&htim8, TIM_CHANNEL_1, pwmValA);
//			  __HAL_TIM_SetCompare(&htim8, TIM_CHANNEL_2, pwmValB);
			  //center();
			  //forward(6000);
			  //stop(10);
			  //sendfeedback();
			  //checkdist(2);
			  i++;
			  //aRxBuffer[0] = '0';
			  break;}
//		  case 'u'://90cm
//			  center();
//			  forward(5900);
//			  stop();
//			  i++;
//			  break;
//		  case 'i'://100
//			  center();
//			  forward(6000);
//			  stop();
//			  i++;
//			  break;
//		  case 'o'://110
//			  center();
//			  forward(6800);
//			  stop();
//			  i++;
//			  break;
//		  case 'p'://120
//			  center();
//			  forward(7100);
//			  stop();
//			  i++;
//			  break;
		  case 's':{
			  center();
			  uint16_t pwmValA=2000;
			  uint16_t pwmValB=1880;//change when charge or on that day
			  HAL_GPIO_WritePin(GPIOA, AIN2_Pin, GPIO_PIN_RESET);
			  HAL_GPIO_WritePin(GPIOA, AIN1_Pin, GPIO_PIN_SET);
			  HAL_GPIO_WritePin(GPIOA, BIN2_Pin, GPIO_PIN_RESET);
			  HAL_GPIO_WritePin(GPIOA, BIN1_Pin, GPIO_PIN_SET);
			  __HAL_TIM_SetCompare(&htim8, TIM_CHANNEL_1, pwmValA);
			  __HAL_TIM_SetCompare(&htim8, TIM_CHANNEL_2, pwmValB);
//			  reverse(350);
//			  stop(500);
//			  sendfeedback();
			  i++;
//			  aRxBuffer[0] = '0';
			  break;}
		  case 'a':{
			  leftmove(900);
			  stop(500);
			  sendfeedback();
			  //checkdist(2);
//			  pwmValA = 100;
//			  pwmValB = 3000;
//			  htim1.Instance->CCR4 = 62;    //extreme left
//			  HAL_GPIO_WritePin(GPIOA, AIN2_Pin, GPIO_PIN_SET);
//			  HAL_GPIO_WritePin(GPIOA, AIN1_Pin, GPIO_PIN_RESET);
//			  HAL_GPIO_WritePin(GPIOA, BIN2_Pin, GPIO_PIN_SET);
//			  HAL_GPIO_WritePin(GPIOA, BIN1_Pin, GPIO_PIN_RESET);
//			  __HAL_TIM_SetCompare(&htim8, TIM_CHANNEL_1, pwmValA);
//			  __HAL_TIM_SetCompare(&htim8, TIM_CHANNEL_2, pwmValB);
//			  osDelay(5000);
//			  htim1.Instance->CCR4 = 75;
//			  pwmValA=0;
//			  pwmValB=0;
//			  HAL_GPIO_WritePin(GPIOA, AIN2_Pin, GPIO_PIN_SET);
//			  HAL_GPIO_WritePin(GPIOA, AIN1_Pin, GPIO_PIN_RESET);
//			  HAL_GPIO_WritePin(GPIOA, BIN2_Pin, GPIO_PIN_SET);
//			  HAL_GPIO_WritePin(GPIOA, BIN1_Pin, GPIO_PIN_RESET);
//			  __HAL_TIM_SetCompare(&htim8, TIM_CHANNEL_1, pwmValA);
//			  __HAL_TIM_SetCompare(&htim8, TIM_CHANNEL_2, pwmValB);
			  i++;
			  //aRxBuffer[0] = '0';
			  break;}
		  case 'd':{
			  rightmove(900);
			  stop(500);
			  sendfeedback();
			  //checkdist(2);
//			  pwmValA = 3000;
//			  pwmValB = 100;
//			  htim1.Instance->CCR4 = 84;    //extreme right
//			  HAL_GPIO_WritePin(GPIOA, AIN2_Pin, GPIO_PIN_SET);
//			  HAL_GPIO_WritePin(GPIOA, AIN1_Pin, GPIO_PIN_RESET);
//			  HAL_GPIO_WritePin(GPIOA, BIN2_Pin, GPIO_PIN_SET);
//			  HAL_GPIO_WritePin(GPIOA, BIN1_Pin, GPIO_PIN_RESET);
//			  __HAL_TIM_SetCompare(&htim8, TIM_CHANNEL_1, pwmValA);
//			  __HAL_TIM_SetCompare(&htim8, TIM_CHANNEL_2, pwmValB);
//			  osDelay(4500);
//			  htim1.Instance->CCR4 = 75;
//			  pwmValA=0;
//			  pwmValB=0;
//			  HAL_GPIO_WritePin(GPIOA, AIN2_Pin, GPIO_PIN_SET);
//			  HAL_GPIO_WritePin(GPIOA, AIN1_Pin, GPIO_PIN_RESET);
//			  HAL_GPIO_WritePin(GPIOA, BIN2_Pin, GPIO_PIN_SET);
//			  HAL_GPIO_WritePin(GPIOA, BIN1_Pin, GPIO_PIN_RESET);
//			  __HAL_TIM_SetCompare(&htim8, TIM_CHANNEL_1, pwmValA);
//			  __HAL_TIM_SetCompare(&htim8, TIM_CHANNEL_2, pwmValB);
			  i++;
			  //aRxBuffer[0] = '0';
			  break;}
		  case 'z':{
			  pwmValA = 300;
			  pwmValB = 2500;
			  htim1.Instance->CCR4 = 67;    //extreme left
			  HAL_GPIO_WritePin(GPIOA, AIN2_Pin, GPIO_PIN_RESET);
			  HAL_GPIO_WritePin(GPIOA, AIN1_Pin, GPIO_PIN_SET);
			  HAL_GPIO_WritePin(GPIOA, BIN2_Pin, GPIO_PIN_RESET);
			  HAL_GPIO_WritePin(GPIOA, BIN1_Pin, GPIO_PIN_SET);
			  __HAL_TIM_SetCompare(&htim8, TIM_CHANNEL_1, pwmValA);
			  __HAL_TIM_SetCompare(&htim8, TIM_CHANNEL_2, pwmValB);
			  osDelay(5000);
			  stop(500);
			  //i++;
			  aRxBuffer[0] = '0';
			  break;}
		  case 'c':{
			  pwmValA = 2500;
			  pwmValB = 300;
			  htim1.Instance->CCR4 = 84;    //extreme right
			  HAL_GPIO_WritePin(GPIOA, AIN2_Pin, GPIO_PIN_RESET);
			  HAL_GPIO_WritePin(GPIOA, AIN1_Pin, GPIO_PIN_SET);
			  HAL_GPIO_WritePin(GPIOA, BIN2_Pin, GPIO_PIN_RESET);
			  HAL_GPIO_WritePin(GPIOA, BIN1_Pin, GPIO_PIN_SET);
			  __HAL_TIM_SetCompare(&htim8, TIM_CHANNEL_1, pwmValA);
			  __HAL_TIM_SetCompare(&htim8, TIM_CHANNEL_2, pwmValB);
			  osDelay(5000);
			  stop(500);
			  //i++;
			  aRxBuffer[0] = '0';
			  break;}
		  case 'x':{
			  stop(500);
			  sendfeedback();
			  //osDelay(5000);
			  i++;
			  //aRxBuffer[0] = '0';
			  break;}
//		  case 'q':// 90 degree left
//			  leftmove(1500);
//			  stop();
//			  i++;
//			  break;
//		  case 'e': //90 degree right
//			  rightmove(1600);
//			  stop();
//			  i++;
//			  break;
//		  case 'r':// 180 degree left
//			  leftmove(2700);
//			  stop();
//			  i++;
//			  break;
//		  case 't': //180 degree right
//			  rightmove(2800);
//			  stop();
//			  i++;
//			  break;
//		  case 'f':// 270 degree left
//			  leftmove(4100);
//			  stop();
//			  i++;
//			  break;
//		  case 'g': //270 degree right
//			  rightmove(4400);
//			  stop();
//			  i++;
//			  break;
//		  case 'v':// 360 degree left
//			  leftmove(5300);
//			  stop();
//			  i++;
//			  break;
//		  case 'b': //360 degree right
//			  rightmove(5000);
//			  stop();
//			  i++;
//			  break;
		  case 'j':{
			  Rrightmove(450);
			  stop(500);
			  //osDelay(500);
			  leftmove(1050);
			  stop(500);
			  //osDelay(500);
			  //reverse(800);//
			  Rrightmove(350);// battery down: 350 else 300
			  stop(500);
			  reverse(300);// time change
			  stop(500);
			  //Rleftmove(250); //remove when battery move down (lab)
			  //Rrightmove(150);//lab
			  Rleftmove(350); // hard ground, orig val = 450 or 400
			  stop(500);         //
			  sendfeedback();
			  //i++;
			  aRxBuffer[0] = '0';
			  break;}
		  case 'k':{
			  Rleftmove(450);//500
			  stop(500);
			  //osDelay(500);
			  rightmove(950); //  1000 (lab orig)
			  stop(500);
			  //osDelay(500);
			  //reverse(800);//
			  Rleftmove(400);// time change(lab) else 250
			  stop(500);
			  reverse(550);//now hard ground time change down: 650 else 850(lab)
			  stop(500);
			  //rightmove(150); // remove (lab)
			  leftmove(200); //hard ground
			  stop(500);         //
			  sendfeedback();
			  //i++;
			  aRxBuffer[0] = '0';
			  break;}
		  case 'n':
			  forwards();
			  stop(50);
			  center();
			  //reverse(350);
			  //stop(500);
			  //leftmove(900);// lab for 3000
			  leftmove(600);
			  stop(50);
			  //forward(350);// lab for 3000
			  forward(450);
			  stop(50);
			  //rightmove(1000);//lab for 3000
			  rightmove(700);
			  stop(50);
			  //rightmove(1200);// lab for 3000
			  rightmove(800);
			  stop(50);
			  forward(980);
			  stop(50);
			  forward(200);//extra for hard ground
			  stop(50);
			  //rightmove(1000);// lab for 3000
			  rightmove(800);
			  stop(50);
			  //rightmove(1400);// lab for 3000
			  rightmove(950); //ori 900 ,> 150cm use 950, else 925
			  stop(50);
			  forward(250);// 200 for below 140, for 140 and above 400
			  //leftmove(1200);// lab for 3000
			  leftmove(850);// ori 800, 750
			  stop(50);
			  forwards();
			  stop(50);
			  forward(350);
			  stop(50);
			  aRxBuffer[0] = '0';
			  break;
		  //}
	  }
    //osDelay(1000);
  }

//	for(;;)
//	{
//		htim1.Instance->CCR4 = 84;   //extreme right84
//		osDelay(5000);
//		htim1.Instance->CCR4 = 75;    //center
//		osDelay(5000);
//		htim1.Instance->CCR4 = 67;    //extreme left
//		osDelay(5000);
//		htim1.Instance->CCR4 = 75;    //center
//		osDelay(5000);
//	}
  /* USER CODE END motors */
}

/* USER CODE BEGIN Header_encoder_task */
/**
* @brief Function implementing the EncoderTask thread.
* @param argument: Not used
* @retval None
*/
/* USER CODE END Header_encoder_task */
void encoder_task(void *argument)
{
  /* USER CODE BEGIN encoder_task */
	HAL_TIM_Encoder_Start(&htim2, TIM_CHANNEL_ALL);
	HAL_TIM_Encoder_Start(&htim3, TIM_CHANNEL_ALL);

	int cnt1, cnt2, diff;
	int cnt3, cnt4, diff1;
	uint32_t tick;
	uint8_t hello[20];
	uint16_t dir, dir1;

	cnt1 = __HAL_TIM_GET_COUNTER(&htim2);
	cnt3 = __HAL_TIM_GET_COUNTER(&htim3);
	tick = HAL_GetTick();
  /* Infinite loop */
  for(;;)
  {
	  if (HAL_GetTick() - tick > 1000L){
		  cnt2 = __HAL_TIM_GET_COUNTER(&htim2);
		  if (__HAL_TIM_IS_TIM_COUNTING_DOWN(&htim2)){
			  if (cnt2 < cnt1)
				  diff = cnt1 - cnt2;
			  else
				  diff = (65535 - cnt2) + cnt1;
		  }
		  else{
			  if (cnt2 > cnt1)
				  diff = cnt2 - cnt1;
			  else
				  diff = (65535 - cnt1) + cnt2;
		  }

		  cnt4 = __HAL_TIM_GET_COUNTER(&htim3);
		  if (__HAL_TIM_IS_TIM_COUNTING_DOWN(&htim3)){
			  if (cnt4 < cnt3)
				  diff1 = cnt3 - cnt4;
			  else
				  diff1 = (65535 - cnt4) + cnt3;
		  }
		  else{
			  if (cnt4 > cnt3)
				  diff1 = cnt4 - cnt3;
			  else
				  diff1 = (65535 - cnt3) + cnt4;
		  }

		  sprintf(hello, "A:%5d", diff);
		  OLED_ShowString(0, 10, hello);
		  sprintf(hello, "B:%5d", diff1);
		  OLED_ShowString(70, 10, hello);
		  dir = __HAL_TIM_IS_TIM_COUNTING_DOWN(&htim2);
		  sprintf(hello, "D: %2d", dir);
		  OLED_ShowString(0, 20, hello);
		  dir1 = __HAL_TIM_IS_TIM_COUNTING_DOWN(&htim3);
		  sprintf(hello, "D1: %2d", dir1);
		  OLED_ShowString(70, 20, hello);
		  cnt1 = __HAL_TIM_GET_COUNTER(&htim2);
		  cnt3 = __HAL_TIM_GET_COUNTER(&htim3);
		  tick = HAL_GetTick();
	  }
    //osDelay(1);
  }
  /* USER CODE END encoder_task */
}

/**
  * @brief  This function is executed in case of error occurrence.
  * @retval None
  */
void Error_Handler(void)
{
  /* USER CODE BEGIN Error_Handler_Debug */
  /* User can add his own implementation to report the HAL error return state */
  __disable_irq();
  while (1)
  {
  }
  /* USER CODE END Error_Handler_Debug */
}

#ifdef  USE_FULL_ASSERT
/**
  * @brief  Reports the name of the source file and the source line number
  *         where the assert_param error has occurred.
  * @param  file: pointer to the source file name
  * @param  line: assert_param error line source number
  * @retval None
  */
void assert_failed(uint8_t *file, uint32_t line)
{
  /* USER CODE BEGIN 6 */
  /* User can add his own implementation to report the file name and line number,
     ex: printf("Wrong parameters value: file %s on line %d\r\n", file, line) */
  /* USER CODE END 6 */
}
#endif /* USE_FULL_ASSERT */

/************************ (C) COPYRIGHT STMicroelectronics *****END OF FILE****/
