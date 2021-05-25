`default_nettype none

`ifdef INTERNAL_TEST

`include "wbdbgbus_uart_rx.sv"
`include "wbdbgbus_uart_tx.sv"
`include "wbdbgbusmaster.sv"
`include "wbdbgbus_fifo.sv"

`endif

module wbdbgbus #(
    parameter CLK_FREQ = 25000000,
    parameter UART_BAUD = 9600,

    // Time before dropping an unfinished instruction
    parameter DROP_CLKS = 2500000, // 0.1s at 25Mhz

    parameter FIFO_DEPTH = 128
)
(
    // UART
    output wire o_tx,
    input wire i_rx,

    // Wishbone
    output wire o_wb_cyc,
    output wire o_wb_stb,
    output wire o_wb_we,
    output wire [31:0] o_wb_addr,
    output wire [31:0] o_wb_data,
    input wire i_wb_ack,
    input wire i_wb_err,
    input wire i_wb_stall,
    input wire [31:0] i_wb_data,
    output wire o_cmd_reset,

    // Interrupts
    input wire i_interrupt_1,
    input wire i_interrupt_2,
    input wire i_interrupt_3,
    input wire i_interrupt_4,
    input wire i_clk
);

localparam RESP_INT_1 = 4'b1000;
localparam RESP_INT_2 = 4'b1001;
localparam RESP_INT_3 = 4'b1010;
localparam RESP_INT_4 = 4'b1011;


// UART
wire [7:0] uart_rx_data;
wire uart_rx_valid;

reg [7:0] uart_tx_data;
wire uart_tx_ready;
reg uart_tx_valid;

uart_rx #(
    .CLK_FREQ(CLK_FREQ),
    .BAUD(UART_BAUD)
) uart_rx (
    .o_data(uart_rx_data),
    .o_valid(uart_rx_valid),

    .i_in(i_rx),
    .i_clk(i_clk)
);

uart_tx #(
    .CLK_FREQ(CLK_FREQ),
    .BAUD(UART_BAUD)
) uart_tx (
    .o_ready(uart_tx_ready),
    .o_out(o_tx),

    .i_data(uart_tx_data),
    .i_valid(uart_tx_valid),
    .i_clk(i_clk)
);

// Wishbone Master
reg cmd_reset;
reg cmd_valid;
wire cmd_ready;
assign o_cmd_reset = cmd_reset;
reg [35:0] cmd_data;

wire resp_valid;
wire [35:0] resp_data;

wbdbgbusmaster wbdbgbusmaster (
    .i_cmd_reset(cmd_reset),
    .i_cmd_valid(cmd_valid),
    .o_cmd_ready(cmd_ready),
    .i_cmd_data(cmd_data),

    .o_resp_valid(resp_valid),
    .o_resp_data(resp_data),

    .o_wb_cyc(o_wb_cyc),
    .o_wb_stb(o_wb_stb),
    .o_wb_we(o_wb_we),
    .o_wb_addr(o_wb_addr),
    .o_wb_data(o_wb_data),
    .i_wb_ack(i_wb_ack),
    .i_wb_err(i_wb_err),
    .i_wb_stall(i_wb_stall),
    .i_wb_data(i_wb_data),

    .i_clk(i_clk)
);

// Command FIFO
reg cmd_fifo_rd_en;
wire [35:0] cmd_fifo_rd_data;
wire cmd_fifo_rd_valid;
reg cmd_fifo_wr_en;
reg [35:0] cmd_fifo_wr_data;
wire cmd_fifo_empty;
wire cmd_fifo_full;

wbdbgbus_fifo #(
    .WIDTH(36),
    .DEPTH(FIFO_DEPTH)
) cmd_fifo (
    .i_rd_en(cmd_fifo_rd_en),
    .o_rd_data(cmd_fifo_rd_data),
    .o_rd_valid(cmd_fifo_rd_valid),
    .i_wr_en(cmd_fifo_wr_en),
    .i_wr_data(cmd_fifo_wr_data),
    .o_empty(cmd_fifo_empty),
    .o_full(cmd_fifo_full),
    .i_clk(i_clk),
    .i_rst(cmd_reset)
);

// Response FIFO
reg resp_fifo_rd_en;
wire [35:0] resp_fifo_rd_data;
wire resp_fifo_rd_valid;
reg resp_fifo_wr_en;
reg [35:0] resp_fifo_wr_data;
wire resp_fifo_empty;
wire resp_fifo_full;

wbdbgbus_fifo #(
    .WIDTH(36),
    .DEPTH(FIFO_DEPTH)
) resp_fifo (
    .i_rd_en(resp_fifo_rd_en),
    .o_rd_data(resp_fifo_rd_data),
    .o_rd_valid(resp_fifo_rd_valid),
    .i_wr_en(resp_fifo_wr_en),
    .i_wr_data(resp_fifo_wr_data),
    .o_empty(resp_fifo_empty),
    .o_full(resp_fifo_full),
    .i_clk(i_clk),
    .i_rst(cmd_reset)
);


reg [39:0] transmit_data;
reg [2:0] transmit_state; // 0 = no tx, 1-5 = bytes

// Interrupt handling
reg interrupt_1_last = 0;
reg interrupt_2_last = 0;
reg interrupt_3_last = 0;
reg interrupt_4_last = 0;

reg interrupt_1_rising = 0;
reg interrupt_2_rising = 0;
reg interrupt_3_rising = 0;
reg interrupt_4_rising = 0;

always_ff @(posedge i_clk) begin
    interrupt_1_last <= i_interrupt_1;
    interrupt_2_last <= i_interrupt_2;
    interrupt_3_last <= i_interrupt_3;
    interrupt_4_last <= i_interrupt_4;
end

// Buffer debug bus output into FIFO
always_ff @(posedge i_clk) begin
    resp_fifo_wr_en <= 0;

    if (resp_valid && ~resp_fifo_full) begin
        resp_fifo_wr_data <= resp_data;
        resp_fifo_wr_en <= 1;
    end
end

// Transmit responses from FIFO
// Also handles interrupts in order to give them priority
always_ff @(posedge i_clk) begin
    uart_tx_valid <= 0;
    resp_fifo_rd_en <= 0;

    // This allows interrupts to be detected even if a transmission
    // is in progress. It is not designed, however, for interrupts
    // which happen very close together (closer than 40 UART-bits together).
    if (i_interrupt_1 && ~interrupt_1_last)
        interrupt_1_rising <= 1;

    if (i_interrupt_2 && ~interrupt_2_last)
        interrupt_2_rising <= 1;

    if (i_interrupt_3 && ~interrupt_3_last)
        interrupt_3_rising <= 1;

    if (i_interrupt_4 && ~interrupt_4_last)
        interrupt_4_rising <= 1;

    // Start FIFO read
    if ((transmit_state == 0) &&
        ~resp_fifo_empty &&
        ~resp_fifo_rd_valid &&
        ~resp_fifo_rd_en &&
        ~interrupt_1_rising &&
        ~interrupt_2_rising &&
        ~interrupt_3_rising &&
        ~interrupt_4_rising) begin

        resp_fifo_rd_en <= 1;
    end

    if (transmit_state == 0) begin
        // FIFO response, triggered by a FIFO read on previous cycle
        if (resp_fifo_rd_valid) begin
            transmit_data <= {4'b0000, resp_fifo_rd_data};
            transmit_state <= 1;
        end
        else if (resp_fifo_rd_en) begin
            // If currently reading from FIFO, don't allow interrupts
        end
        else if (interrupt_1_rising) begin
            transmit_data <= {4'b0000, RESP_INT_1, 32'b0};
            transmit_state <= 1;
            interrupt_1_rising <= 0;
        end
        else if (interrupt_2_rising) begin
            transmit_data <= {4'b0000, RESP_INT_2, 32'b0};
            transmit_state <= 1;
            interrupt_2_rising <= 0;
        end
        else if (interrupt_3_rising) begin
            transmit_data <= {4'b0000, RESP_INT_3, 32'b0};
            transmit_state <= 1;
            interrupt_3_rising <= 0;
        end
        else if (interrupt_4_rising) begin
            transmit_data <= {4'b0000, RESP_INT_4, 32'b0};
            transmit_state <= 1;
            interrupt_4_rising <= 0;
        end
    end
    else begin
        if (uart_tx_ready && ~uart_tx_valid) begin
            case (transmit_state)
                1: uart_tx_data <= transmit_data[39:32];
                2: uart_tx_data <= transmit_data[31:24];
                3: uart_tx_data <= transmit_data[23:16];
                4: uart_tx_data <= transmit_data[15:8];
                5: uart_tx_data <= transmit_data[7:0];
                default: uart_tx_data <= 0;
            endcase

            uart_tx_valid <= 1;

            transmit_state <= transmit_state + 1;

            if (transmit_state == 5) begin
                transmit_state <= 0;
            end
        end
    end
end

reg [39:0] recieve_data;
reg [2:0] recieve_state; // 0-4 = bytes, 5 = stalled

// Countdown to dropping un-finished instruction
/* verilator lint_off WIDTH */
reg [$clog2(DROP_CLKS):0] drop_timer = DROP_CLKS;
/* verilator lint_on WIDTH */

// Recieve commands and add to command FIFO
always_ff @(posedge i_clk) begin
    cmd_reset <= 0;
    cmd_fifo_wr_en <= 0;

    if (uart_rx_valid) begin
        case (recieve_state)
            0: recieve_data[39:32] <= uart_rx_data;
            1: recieve_data[31:24] <= uart_rx_data;
            2: recieve_data[23:16] <= uart_rx_data;
            3: recieve_data[15:8] <= uart_rx_data;
            4: recieve_data[7:0] <= uart_rx_data;
        endcase

        recieve_state <= recieve_state + 1;
        /* verilator lint_off WIDTH */
        drop_timer <= DROP_CLKS;
        /* verilator lint_on WIDTH */

        if (recieve_state == 4) begin
            // Reset
            if (recieve_data[35:32] == 4'b1111) begin
                cmd_reset <= 1;
                recieve_state <= 0;
            end
            else begin
                if (~cmd_fifo_full) begin
                    cmd_fifo_wr_en <= 1;
                    cmd_fifo_wr_data <= {recieve_data[35:8], uart_rx_data};
                end
                recieve_state <= 0;
            end
        end
    end
    else if (recieve_state > 0) begin
        drop_timer <= drop_timer - 1;

        if (drop_timer == 1) begin
            recieve_state <= 0;
        end
    end
end

// Read from command FIFO and forward to bus
always_ff @(posedge i_clk) begin
    cmd_valid <= 0;
    cmd_fifo_rd_en <= 0;

    if (~cmd_reset && cmd_ready && ~cmd_fifo_empty &&
        ~cmd_fifo_rd_en && ~cmd_fifo_rd_valid && ~cmd_valid) begin
        cmd_fifo_rd_en <= 1;
    end

    if (cmd_ready && cmd_fifo_rd_valid && ~cmd_reset) begin
        cmd_valid <= 1;
        cmd_data <= cmd_fifo_rd_data;
    end
end

endmodule
