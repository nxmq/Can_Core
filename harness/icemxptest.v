`default_nettype none
module top(clki,rp,gp,bp);
    input clki;
    output rp;
    output gp;
    output bp;
    reg [23:0] counter;
    reg [2:0] ledset;
    wire clkosc;
    wire clk;
    assign clk = clkosc;
    SB_GB clk_gb (
        .USER_SIGNAL_TO_GLOBAL_BUFFER(clki),
        .GLOBAL_BUFFER_OUTPUT(clkosc)
    );

    initial begin
    counter <= 24'b0;
    end

    always @ (posedge clk) begin
        counter <= counter + 1'b1;
        if (counter > 16000000) begin
            counter <= 24'b0;
            if(ledset == 3'b111) 
                ledset <= 3'b000;
            else
                ledset <= ledset + 3'b001;
        end
    end

    SB_RGBA_DRV RGBA_DRIVER (
        .CURREN(1'b1),
        .RGBLEDEN(1'b1),
        .RGB0PWM(ledset[0]),       // Red
        .RGB1PWM(ledset[2]),       // Green
        .RGB2PWM(ledset[1]),       // Blue
        .RGB0(rp),
        .RGB1(gp),
        .RGB2(bp)
    );

    localparam RGBA_CURRENT_MODE_FULL = "0b0";
    localparam RGBA_CURRENT_MODE_HALF = "0b1";

    localparam RGBA_CURRENT_04MA_02MA = "0b000001";
    localparam RGBA_CURRENT_08MA_04MA = "0b000011";
    localparam RGBA_CURRENT_12MA_06MA = "0b000111";
    localparam RGBA_CURRENT_16MA_08MA = "0b001111";
    localparam RGBA_CURRENT_20MA_10MA = "0b011111";
    localparam RGBA_CURRENT_24MA_12MA = "0b111111";

    defparam RGBA_DRIVER.CURRENT_MODE = RGBA_CURRENT_MODE_HALF;
    defparam RGBA_DRIVER.RGB0_CURRENT = RGBA_CURRENT_04MA_02MA;  // Blue
    defparam RGBA_DRIVER.RGB1_CURRENT = RGBA_CURRENT_04MA_02MA;  // Red
    defparam RGBA_DRIVER.RGB2_CURRENT = RGBA_CURRENT_04MA_02MA;  // Green

endmodule 

