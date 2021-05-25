`default_nettype none

module wbdbgbus_testharness #(
    parameter CLK_FREQ = 16000000,
    parameter UART_BAUD = 115200,
    parameter DROP_CLKS = 1600000) (
    output wire rio_io_14,
    input wire rio_io_15,
    input wire rio_io_5,
    output wire rio_io_4,
    input wire clki
);
wire wb_cyc, wb_stb, wb_we, irq, rst,clk;
reg wb_ack;
wire [31:0] wb_addr;
wire [31:0] wb_wdata;
reg [7:0] wb_rdata;

/* verilator lint_off PINMISSING */
wbdbgbus #(
    .CLK_FREQ(CLK_FREQ),
    .UART_BAUD(UART_BAUD),
    .DROP_CLKS(DROP_CLKS)
) wbdbgbus (
    .o_tx(rio_io_14),
    .i_rx(rio_io_15),
    .o_cmd_reset(rst),
    .o_wb_cyc(wb_cyc),
    .o_wb_stb(wb_stb),
    .o_wb_we(wb_we),
    .o_wb_addr(wb_addr),
    .o_wb_data(wb_wdata),
    .i_wb_ack(wb_ack),
    .i_wb_err(1'b0),
    .i_wb_data({24'b0,wb_rdata[7:0]}),
    .i_interrupt_1(!irq),
    .i_interrupt_2(1'b0),
    .i_interrupt_3(1'b0),
    .i_interrupt_4(1'b0),
    .i_wb_stall(1'b0),
    .i_clk(clki)
);
CanTop canTop(
    .reset(rst),
    .io_wbClkI(clki),
    .clock(clki),
    .io_wbDatI(wb_wdata[7:0]),
    .io_wbDatO(wb_rdata[7:0]),
    .io_wbCycI(wb_cyc),
    .io_wbStbI(wb_stb),
    .io_wbWeI(wb_we),
    .io_wbAddrI(wb_addr[7:0] - 8'b1),
    .io_wbAckO(wb_ack),
    .io_irqOn(irq),
    .io_canTx(rio_io_4),
    .io_canRx(rio_io_5),
);

endmodule
