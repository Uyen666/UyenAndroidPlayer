"""
UyenController PC Receiver (UDP to Virtual Xbox 360 Gamepad)
============================================================
Commercial-Grade Companion Script for UyenLauncher

Receives low-latency UDP gamepad state packets from the UyenLauncher Android app
(port 8999) and simulates a physical Microsoft Xbox 360 controller on Windows
via the ViGEmBus driver.

Prerequisites:
    pip install -r requirements.txt
    (vgamepad will automatically install or verify the ViGEmBus driver)

Usage:
    python uyen_controller_receiver.py [--port 8999]
"""

import socket
import json
import sys
import argparse

try:
    import vgamepad as vg
    HAS_VGAMEPAD = True
except ImportError:
    HAS_VGAMEPAD = False


def get_local_ips():
    """Returns a list of local IPv4 addresses for easy connection setup."""
    ip_list = []
    try:
        host_name = socket.gethostname()
        for ip in socket.gethostbyname_ex(host_name)[2]:
            if not ip.startswith("127."):
                ip_list.append(ip)
    except Exception:
        pass
    if not ip_list:
        ip_list.append("127.0.0.1")
    return ip_list


def main():
    parser = argparse.ArgumentParser(description="UyenController PC Receiver")
    parser.add_argument("--port", type=int, default=8999, help="UDP listening port (default: 8999)")
    args = parser.parse_args()

    local_ips = get_local_ips()
    print("=" * 60)
    print("🎮 UyenController PC Receiver - Virtual Gamepad Companion")
    print("=" * 60)
    print(f"[*] Listening on: 0.0.0.0:{args.port}")
    print("[*] Detected PC Local IP Address(es):")
    for ip in local_ips:
        print(f"    👉 {ip}")
    print("\n[!] Please make sure your phone and PC are on the same Wi-Fi network")
    print(f"    or use 'adb forward udp:{args.port} udp:{args.port}' via USB.")
    print("=" * 60)

    gamepad = None
    if HAS_VGAMEPAD:
        try:
            gamepad = vg.VX360Gamepad()
            print("✅ Virtual Xbox 360 Controller initialized successfully!")
            print("   Games (Steam, Epic, Emulators) will recognize this as a real Xbox 360 controller.")
        except Exception as e:
            print(f"⚠️ Failed to create virtual gamepad via vgamepad: {e}")
            print("   Falling back to packet monitor / console inspection mode.")
            gamepad = None
    else:
        print("⚠️ 'vgamepad' package is not installed.")
        print("   Running in CONSOLE INSPECTION mode.")
        print("   To enable real controller output in Windows games:")
        print("       pip install vgamepad")
    print("=" * 60)
    print("[*] Waiting for controller packets from UyenLauncher on phone...\n")

    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.bind(("0.0.0.0", args.port))

    packet_count = 0

    try:
        while True:
            data, addr = sock.recvfrom(2048)
            packet_count += 1
            try:
                state = json.loads(data.decode("utf-8"))
            except Exception:
                continue

            # Extract fields
            lx = float(state.get("lx", 0.0))
            ly = float(state.get("ly", 0.0))
            rx = float(state.get("rx", 0.0))
            ry = float(state.get("ry", 0.0))

            btn_a = bool(state.get("a", False))
            btn_b = bool(state.get("b", False))
            btn_x = bool(state.get("x", False))
            btn_y = bool(state.get("y", False))

            dup = bool(state.get("dup", False))
            ddown = bool(state.get("ddown", False))
            dleft = bool(state.get("dleft", False))
            dright = bool(state.get("dright", False))

            l1 = bool(state.get("l1", False))
            r1 = bool(state.get("r1", False))
            l2 = float(state.get("l2", 0.0))
            r2 = float(state.get("r2", 0.0))

            btn_start = bool(state.get("start", False))
            btn_select = bool(state.get("select", False))

            if gamepad:
                # Analog sticks (-1.0 to 1.0, Y is inverted for standard gamepad axes)
                gamepad.left_joystick_float(x_value_float=lx, y_value_float=-ly)
                gamepad.right_joystick_float(x_value_float=rx, y_value_float=-ry)

                # Triggers (0.0 to 1.0)
                gamepad.left_trigger_float(value_float=l2)
                gamepad.right_trigger_float(value_float=r2)

                # Buttons
                buttons_to_update = [
                    (btn_a, vg.XUSB_BUTTON.XUSB_GAMEPAD_A),
                    (btn_b, vg.XUSB_BUTTON.XUSB_GAMEPAD_B),
                    (btn_x, vg.XUSB_BUTTON.XUSB_GAMEPAD_X),
                    (btn_y, vg.XUSB_BUTTON.XUSB_GAMEPAD_Y),
                    (l1, vg.XUSB_BUTTON.XUSB_GAMEPAD_LEFT_SHOULDER),
                    (r1, vg.XUSB_BUTTON.XUSB_GAMEPAD_RIGHT_SHOULDER),
                    (btn_start, vg.XUSB_BUTTON.XUSB_GAMEPAD_START),
                    (btn_select, vg.XUSB_BUTTON.XUSB_GAMEPAD_BACK),
                    (dup, vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_UP),
                    (ddown, vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_DOWN),
                    (dleft, vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_LEFT),
                    (dright, vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_RIGHT),
                ]

                for is_pressed, xusb_btn in buttons_to_update:
                    if is_pressed:
                        gamepad.press_button(button=xusb_btn)
                    else:
                        gamepad.release_button(button=xusb_btn)

                gamepad.update()

            if packet_count % 30 == 0 or any([btn_a, btn_b, btn_x, btn_y, l1, r1]):
                active_btns = []
                if btn_a: active_btns.append("A")
                if btn_b: active_btns.append("B")
                if btn_x: active_btns.append("X")
                if btn_y: active_btns.append("Y")
                if l1: active_btns.append("L1")
                if r1: active_btns.append("R1")
                if l2 > 0.5: active_btns.append(f"L2({l2:.1f})")
                if r2 > 0.5: active_btns.append(f"R2({r2:.1f})")
                btn_str = "+".join(active_btns) if active_btns else "None"
                print(f"\r[UyenController] Packets: {packet_count} | Sticks: L({lx:+.2f}, {ly:+.2f}) R({rx:+.2f}, {ry:+.2f}) | Buttons: {btn_str:<16}", end="", flush=True)

    except KeyboardInterrupt:
        print("\n[*] Shutting down UyenController Receiver. Goodbye!")
    finally:
        sock.close()


if __name__ == "__main__":
    main()
