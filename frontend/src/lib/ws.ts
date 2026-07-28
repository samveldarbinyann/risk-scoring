import { Client, type IMessage } from "@stomp/stompjs";
import type { ScanProgressMessage } from "@/lib/types";

const WS_URL: string = import.meta.env.VITE_WS_URL ?? "ws://localhost:8081/ws";

export function subscribeScanGroupProgress(
  groupId: string,
  onMessage: (message: ScanProgressMessage) => void,
): () => void {
  const client = new Client({
    brokerURL: WS_URL,
    reconnectDelay: 3000,
  });

  client.onConnect = () => {
    client.subscribe(`/topic/scan-groups/${groupId}`, (frame: IMessage) => {
      onMessage(JSON.parse(frame.body) as ScanProgressMessage);
    });
  };

  client.activate();

  return () => {
    void client.deactivate();
  };
}
