`default_nettype none

// UART Reciever (8/N/1)
module uart_rx
#(
    parameter CLK_FREQ = 250000,
    parameter BAUD = 9600
)
(
    output reg [7:0] o_data,
    output reg o_valid,

    input wire i_in,
    input wire i_clk
); 

parameter CLKS_PER_BIT = CLK_FREQ / BAUD;
parameter CLKS_PER_1_5_BIT = 3 * CLKS_PER_BIT / 2;

reg[$clog2(CLKS_PER_BIT * 2):0] counter;
reg [3:0] state = 0; // 0 = idle, 1 = start bit, 2-9 = data bits

always_ff @(posedge i_clk) begin
    o_valid <= 0;
    counter <= 10; // Set counter to default value when idle

    if(state == 0) begin
        // Start bit
        if(i_in == 0) begin
            state <= 1;
            /* verilator lint_off WIDTH */
            counter <= CLKS_PER_1_5_BIT;
            /* verilator lint_on WIDTH */
        end

        // Else stay in idle
    end
    else if (counter == 0) begin
        // End bit
        if(state == 9) begin
            if (i_in == 1) begin
                o_valid <= 1;
            end
            state <= 0;
        end

        // Data bits
        else begin
            state <= state + 1;
            o_data[state - 1] <= i_in;

            /* verilator lint_off WIDTH */
            counter <= CLKS_PER_BIT;
            /* verilator lint_on WIDTH */
        end
    end
    else begin
        counter <= counter - 1;
    end
end

endmodule
