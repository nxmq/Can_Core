`default_nettype none

module wbdbgbusmaster (
    // Debug port connection
    input wire i_cmd_reset,
    input wire i_cmd_valid,
    output wire o_cmd_ready,
    input wire [35:0] i_cmd_data,
    
    output reg o_resp_valid = 0,
    output reg [35:0] o_resp_data,

    // Wishbone bus connection
    output reg o_wb_cyc,
    output reg o_wb_stb,
    output reg o_wb_we,
    output reg [31:0] o_wb_addr,
    output reg [31:0] o_wb_data,
    input wire i_wb_ack,
    input wire i_wb_err,
    input wire i_wb_stall,
    input wire [31:0] i_wb_data,

    input wire i_clk
);

localparam CMD_READ_REQ     = 4'b0001;
localparam CMD_WRITE_REQ    = 4'b0010;
localparam CMD_SET_ADDR     = 4'b0011;
localparam CMD_SET_ADDR_INC = 4'b0111;

localparam RESP_READ_RESP   = 4'b0001;
localparam RESP_WRITE_ACK   = 4'b0010;
localparam RESP_ADDR_ACK    = 4'b0011;
localparam RESP_BUS_ERROR   = 4'b0100;
localparam RESP_BUS_RESET   = 4'b0101;

reg addr_inc;

assign o_cmd_ready = (~o_wb_cyc);

wire cmd_recv = i_cmd_valid && o_cmd_ready;
wire [3:0] cmd_inst = i_cmd_data[35:32];
wire [31:0] cmd_data = i_cmd_data[31:0];

// Bus state
always_ff @(posedge i_clk) begin
    if (i_wb_err || i_cmd_reset) begin
        // Reset on error or bus-reset
        o_wb_cyc <= 0;
        o_wb_stb <= 0;
    end
    else if (o_wb_stb) begin
        // If not stalled, output is complete
        if (!i_wb_stall) begin
            o_wb_stb <= 0;
        end
    end
    else if (o_wb_cyc) begin
        // Once acknowledged, finish cycle
        if (i_wb_ack) begin
            o_wb_cyc <= 0;
        end
    end
    else begin
        if (cmd_recv && 
            ((cmd_inst == CMD_READ_REQ) || (cmd_inst == CMD_WRITE_REQ))) begin

            o_wb_cyc <= 1;
            o_wb_stb <= 1;
        end
    end
end

// Addressing
always_ff @(posedge i_clk) begin
    if (cmd_recv) begin
        if ((cmd_inst == CMD_SET_ADDR) || (cmd_inst == CMD_SET_ADDR_INC)) begin
            o_wb_addr <= cmd_data;
            addr_inc <= (cmd_inst == CMD_SET_ADDR_INC);
        end
    end
    else if (o_wb_stb && ~i_wb_stall) begin
        /* verilator lint_off WIDTH */
        o_wb_addr <= o_wb_addr + addr_inc;
        /* verilator lint_on WIDTH */
    end
end

// Write-Enable
always_ff @(posedge i_clk) begin
    // Allow for stalling
    if(~o_wb_cyc) begin
        o_wb_we <= cmd_recv && (cmd_inst == CMD_WRITE_REQ);
    end
end

// Write Data
always_ff @(posedge i_clk) begin
    // Update data when not stalled
    if (~(o_wb_stb && i_wb_stall)) begin
        o_wb_data <= cmd_data;
    end
end

// Acknowledgement / response
always_ff @(posedge i_clk) begin
    o_resp_valid <= 0;

    if (i_cmd_reset) begin
        o_resp_valid <= 1;
        o_resp_data <= {RESP_BUS_RESET, 32'b0};
    end
    else if (i_wb_err) begin
        o_resp_valid <= 1;
        o_resp_data <= {RESP_BUS_ERROR, 32'b0};
    end
    else if (o_wb_cyc && i_wb_ack) begin
        o_resp_valid <= 1;
        if (o_wb_we) begin
            o_resp_data <= {RESP_WRITE_ACK, 32'b0};
        end
        else begin
            o_resp_data <= {RESP_READ_RESP, i_wb_data};
        end
    end
    else if (cmd_recv && 
        ((cmd_inst == CMD_SET_ADDR) || (cmd_inst == CMD_SET_ADDR_INC))) begin
        o_resp_valid <= 1;
        o_resp_data <= {RESP_ADDR_ACK, 32'b0};
    end
end

endmodule