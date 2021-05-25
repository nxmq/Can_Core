package engineer.woke.cancore

import chisel3._
import chisel3.util._

//VERIFIED IDENTICAL TO can_btl.v VIA SAT SOLVE ON 4/28/2021, DONT FUCKING TOUCH

/* BTL TIMING OVERVIEW:
NOMINAL BIT IS 8 TIME QUANTA (TQ) PER BIT, W/ SAMPLE TAKEN AT 75% MARK (6 TQ IN)
IN REALITY, 16 TQ/BIT IS MORE COMMON, WITH SAMPLE TAKEN AT 87.5% MARK (14 TQ IN)
THERE ARE 4 SEGMENTS TO A BIT: SYNC SEG, PROP SEG, PHASE 1 SEG, AND PHASE 2 SEG.
SYNC SEG: FOR ALL BUS NODES TO SYNCHRONIZE. MANDATED TO BE 1 TQ LONG IN STANDARD
PROP SEG: ALLOWS SIGNAL TO PROP. THROUGH NETWORK. 1/2/4/8 TQ LONG, TYP 1 TQ LONG
PHASE 1 SEG: FOR SAMPLING PHASE COMP. 1/2/4/8 TQ LONG, TYP 4. CAN GROW IN RESYNC
PHASE 2 SEG: REMAINDER OF BIT. SJW TO LENGTH(PH1) TQ LONG. MAY SHORTEN IN RESYNC
SJW (SYNC JUMP WIDTH): AMOUNT BY WHICH THE PH1/PH2 SPLIT CAN BE VARIED IN RESYNC
TOTAL QUANTA PER BIT SUPPORTED BY THIS CONTROLLER IS THE STANDARD 8-25 TQ RANGE.

TYPICAL BIT ACQUISITION: (SINGLE SAMPLING)

                                                   SAMPLE HERE
                                                         │
                                                         ↓
▔▔╲ ╱▔▔▔▔▔▔▔║▔▔▔▔▔▔▔▔║▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔║▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔╲ ╱▔▔▔
   ╳  SYNC  ║  PROP  ║           PHASE 1 SEG             ║  PHASE 2 SEG    ╳
▁▁╱ ╲▁▁▁▁▁▁▁║▁▁▁▁▁▁▁▁║▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁║▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁╱ ╲▁▁▁
   ⇧        ⇧        ⇧        ⇧        ⇧        ⇧        ⇧        ⇧        ⇧
   0        1        2        3        4        5        6        7        8

TYPICAL BIT ACQUISITION: (TRIPLE SAMPLING)

                                                SAMPLE AT ALL THREE
                                                │        │        │
                                                ↓        ↓        ↓
▔▔╲ ╱▔▔▔▔▔▔▔║▔▔▔▔▔▔▔▔║▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔║▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔╲ ╱▔▔▔
   ╳  SYNC  ║  PROP  ║           PHASE 1 SEG             ║  PHASE 2 SEG    ╳
▁▁╱ ╲▁▁▁▁▁▁▁║▁▁▁▁▁▁▁▁║▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁║▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁╱ ╲▁▁▁
   ⇧        ⇧        ⇧        ⇧        ⇧        ⇧        ⇧        ⇧        ⇧
   0        1        2        3        4        5        6        7        8

MAJORITY VALUE OF THE SAMPLES IS USED IN TRIPLE SAMPLE MODE TO SUPPRESS EMI.
 */


class CanBtl extends Module with RequireAsyncReset {
  val io = IO(new Bundle {
    // CAN RXD Line
    val rx : Bool = Input(Bool())
    // CAN TXD Line
    val tx : Bool = Input(Bool())
    // BRP Register Value
    val baudRatePrescaler : UInt = Input(UInt(6.W))
    // SJW Register Value
    val syncJumpWidth : UInt = Input(UInt(2.W))
    // Width of Time Segment 1 in TQ
    val timeSegment1 : UInt = Input(UInt(4.W))
    // Width of Time Segment 2 in TQ
    val timeSegment2 : UInt = Input(UInt(3.W))
    // Triple Sampling Enable Bit
    val tripleSampling : Bool = Input(Bool())
    // Rx State Machine Idle Status Bit
    val rxIdle : Bool = Input(Bool())
    // Rx State Machine Interframe Status Bit
    val rxInter : Bool = Input(Bool())
    // Currently performing Transmission
    val transmitting : Bool = Input(Bool())
    // Intend to Transmit a Message
    val transmitter : Bool = Input(Bool())
    // Rx State Machine Transition to Interframe State
    val goRxInter : Bool = Input(Bool())
    // Next value for TXD Output
    val txNext : Bool = Input(Bool())
    // Tx State Machine Transition to Overload Frame State
    val goOverloadFrame : Bool = Input(Bool())
    // Tx State Machine Transition to Error Frame State
    val goErrorFrame : Bool = Input(Bool())
    // Tx State Machine Transition to Transmit State
    val goTx : Bool = Input(Bool())
    // Are we going to ack a message on the bus?
    val sendAck : Bool = Input(Bool())
    // Is the controller in Error Passive State?
    val nodeErrorPassive : Bool = Input(Bool())
    // Goes high when its time to sample
    val samplePoint : Bool = Output(Bool())
    // State of RXD captured at last samplePoint
    val sampledBit : Bool = Output(Bool())
    // prior value of sampledBit
    val sampledBitQ : Bool = Output(Bool())
    // Indicates start of new bit frame
    val txPoint : Bool = Output(Bool())
    // Indicates hard sync point at falling edge of SOF
    val hardSync : Bool = Output(Bool())
  })

  val samplePoint : Bool = RegInit(false.B)
  io.samplePoint := samplePoint
  val txPoint : Bool = RegInit(false.B)
  io.txPoint := txPoint
  val sampledBit : Bool = RegInit(true.B)
  io.sampledBit := sampledBit
  val sampledBitQ : Bool = RegInit(true.B)
  io.sampledBitQ := sampledBitQ

  val clockCount : UInt = RegInit(0.U(7.W))

  val clockEn : Bool = RegInit(false.B)

  val clockEnQ : Bool = RegNext(clockEn,false.B)

  val syncBlocked : Bool = RegInit(true.B)

  val hardSyncBlocked : Bool = RegInit(false.B)
  // Quanta Counter for tracking progress through bit.
  val quantaCounter : UInt = RegInit(0.U(5.W))

  val delay : UInt = RegInit(0.U(4.W))

  val sync : Bool = RegInit(false.B)

  val seg1 : Bool = RegInit(true.B)

  val seg2 : Bool = RegInit(false.B)

  val resyncLatched : Bool = RegInit(false.B)

  val sample : UInt = RegInit(3.U(2.W))

  val txNextSp: Bool = RegInit(false.B)

  val syncWindow : Bool = Wire(Bool())

  val prescalerLimit : UInt = Wire(UInt(8.W))

  val resync : Bool = Wire(Bool())

  val goSync : Bool = Wire(Bool())
  // When the clock is enabled, and we are synchronized, go to seg1.
  val goSeg1 : Bool = clockEnQ & (sync | io.hardSync | (resync & seg2 & syncWindow) | (resyncLatched & syncWindow))
  // When the clock is enabled, seg1 is active, and we are not hard-syncing
  // wait until the quanta Counter reaches the end of seg1, and proceed to seg2.
  val goSeg2 : Bool = clockEnQ & (seg1 & (!io.hardSync) & (quantaCounter === (io.timeSegment1 +& delay)))
  // prescaler limit is equal to 2*(BRP + 1)
  prescalerLimit := (io.baudRatePrescaler + 1.U) << 1
  // hard sync occurs when the rx line goes low in the idle/interframe region,
  // and the current frame is in a valid state for hard-syncing.
  io.hardSync := (io.rxIdle | io.rxInter) & (!io.rx) & io.sampledBit & (!hardSyncBlocked)
  // normal resync occurs when the line goes low during an actual frame,
  resync := !io.rxIdle & !io.rxInter & !io.rx & io.sampledBit & !syncBlocked
  goSync := clockEnQ & seg2 & (quantaCounter(2,0) === io.timeSegment2) & (!io.hardSync) & (!resync)

  syncWindow := ((io.timeSegment2 - quantaCounter(2,0)) < (io.syncJumpWidth + 1.U(4.W)))

  txPoint := !txPoint & seg2 & ( clockEn & (quantaCounter(2,0) === io.timeSegment2) | (clockEn | clockEnQ) & (resync | io.hardSync))

  when(clockCount >= (prescalerLimit - 1.U)) {
    clockCount := 0.U
  }.otherwise {
    clockCount := clockCount + 1.U
  }

  when(clockCount === (prescalerLimit - 1.U)) {
    clockEn := true.B
  }.otherwise {
    clockEn := false.B
  }

  when(resync & seg2 & !syncWindow) {
    resyncLatched := true.B
  }.elsewhen(goSeg1) {
    resyncLatched := false.B
  }

  when(clockEnQ) {
    sync := goSync
    sample := Cat(sample(0), io.rx)
  }

  when(goSeg1) {
    // Transition to seg1 when the transition flag goes high.
    seg1 := true.B
  }.elsewhen(goSeg2) {
    // Exit seg1 upon seg2 entry
    seg1 := false.B
  }

  when(goSeg2) {
    seg2 := true.B
  }.elsewhen(goSync | goSeg1) {
    seg2 := false.B
  }

  when(goSync | goSeg1 | goSeg2) {
    // Reset quanta counter to zero when we synchronize, or do a segment transition.
    quantaCounter := 0.U
  }.elsewhen(clockEnQ) {
    // When clock enable is latched, increment quanta counter.
    quantaCounter := quantaCounter +& 1.U
  }

  when(resync & seg1 & (!io.transmitting | io.transmitting & (txNextSp | (io.tx & !io.rx)))) {
    delay := Mux(quantaCounter > io.syncJumpWidth,io.syncJumpWidth + 1.U(4.W),quantaCounter +& 1.U)
  }
  .elsewhen(goSync | goSeg1) {
    delay := 0.U
  }

  when(io.goErrorFrame) {
    sampledBitQ := sampledBit
    samplePoint := false.B
  }
  .elsewhen(clockEnQ & !io.hardSync) {
    when(seg1 & (quantaCounter === (io.timeSegment1 +& delay))) {
      when(io.tripleSampling) {
        sampledBit := sample.andR() | (sample(0) & io.rx) | (sample(1) & io.rx)
      }.otherwise {
        sampledBit := Mux(io.rx,true.B,false.B)
      }
      samplePoint := true.B
      sampledBitQ := sampledBit
    }
  }.otherwise {
    samplePoint := false.B
  }

  when(io.goOverloadFrame | (io.goErrorFrame & !io.nodeErrorPassive) | io.goTx | io.sendAck) {
    txNextSp := false.B
  }
  .elsewhen(io.goErrorFrame & io.nodeErrorPassive) {
    txNextSp := true.B
  }
  .elsewhen(samplePoint) {
    txNextSp := io.txNext
  }

  when(clockEnQ) {
    when(resync) {
      syncBlocked := true.B
    }.elsewhen(goSeg2) {
      syncBlocked := false.B
    }
  }

  when(io.hardSync & clockEnQ | (io.transmitting & io.transmitter | io.goTx) & io.txPoint & (~io.txNext)) {
    hardSyncBlocked := true.B
  }
  .elsewhen(io.goRxInter | (io.rxIdle | io.rxInter) & samplePoint & io.sampledBit) {
    hardSyncBlocked := false.B
  }
}

