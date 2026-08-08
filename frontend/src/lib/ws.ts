import { Client, type IFrame, type IMessage } from "@stomp/stompjs";
import { getAccessToken } from "@/lib/api";
import type { ScanProgressMessage } from "@/lib/types";

const WS_URL: string = import.meta.env.VITE_WS_URL ?? "ws://localhost:8081/ws";

export function subscribeScanGroupProgress(
  groupId: string,
  onMessage: (message: ScanProgressMessage) => void,
  onError: (reason: string | undefined) => void,
): () => void {
  const client = new Client({
    brokerURL: WS_URL,
    reconnectDelay: 3000,
  });

  client.beforeConnect = () => {
    const token = getAccessToken();
    client.connectHeaders = token ? { Authorization: `Bearer ${token}` } : {};
  };

  client.onConnect = () => {
    client.subscribe(`/topic/scan-groups/${groupId}`, (frame: IMessage) => {
      onMessage(JSON.parse(frame.body) as ScanProgressMessage);
    });
  };

  client.onStompError = (frame: IFrame) => {
    void client.deactivate();
    onError(frame.headers.message);
  };

  client.activate();

  return () => {
    void client.deactivate();
  };
}
