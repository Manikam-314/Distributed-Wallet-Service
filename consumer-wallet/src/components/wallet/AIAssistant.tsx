import { useState, useRef, useEffect } from "react";
import { Sparkles, Send, X, Bot, User, Loader2, AlertCircle } from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";
import { agentAPI, type ChatResponse } from "@/services/agentAPI";
import { useWalletStore } from "@/store/walletStore";
import { walletAPI } from "@/services/walletAPI";
import { Button } from "@/components/ui/button";

interface Message {
  id: string;
  sender: "user" | "bot";
  text: string;
  timestamp: Date;
  tool?: string | null;
  status?: string | null;
  confirmationRequired?: boolean;
}

export function AIAssistant() {
  const [isOpen, setIsOpen] = useState(false);
  const [messages, setMessages] = useState<Message[]>([
    {
      id: "welcome",
      sender: "bot",
      text: "Hello! I am your **PayVault AI Assistant**, powered by Spring AI & Gemini.\n\nI can help you:\n* 💳 **Check your wallet balance**\n* 📜 **View your transaction history**\n* 💸 **Initiate money transfers** (e.g., *'Send 500 to Thai'*)\n\nWhat can I do for you today?",
      timestamp: new Date()
    }
  ]);
  const [input, setInput] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [isConfirmationPending, setIsConfirmationPending] = useState(false);
  
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const { wallet, setWallet } = useWalletStore();

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  useEffect(() => {
    if (isOpen) {
      setTimeout(scrollToBottom, 100);
    }
  }, [messages, isOpen]);

  const handleSendMessage = async (textToSend: string, isConfirmationAction: boolean = false) => {
    if (!textToSend.trim()) return;

    // Add user message
    const userMsg: Message = {
      id: Math.random().toString(),
      sender: "user",
      text: textToSend,
      timestamp: new Date()
    };
    setMessages(prev => [...prev, userMsg]);
    setInput("");
    setIsLoading(true);

    try {
      let responseData: ChatResponse;

      if (isConfirmationAction) {
        responseData = await agentAPI.confirm(textToSend);
        setIsConfirmationPending(false);
      } else {
        responseData = await agentAPI.chat(textToSend);
      }

      // Add bot message
      const botMsg: Message = {
        id: Math.random().toString(),
        sender: "bot",
        text: responseData.response,
        timestamp: new Date(),
        tool: responseData.tool,
        status: responseData.status,
        confirmationRequired: responseData.confirmationRequired
      };

      setMessages(prev => [...prev, botMsg]);

      if (responseData.confirmationRequired) {
        setIsConfirmationPending(true);
      }

      // If the balance changed (e.g. transfer executed, deposit, etc.), update wallet state in store
      if (responseData.tool === "TransferMoneyTool" && responseData.status === "SUCCESS") {
        if (wallet) {
          const updatedWallet = await walletAPI.getWalletByUserId(wallet.userId);
          setWallet(updatedWallet);
        }
      }
    } catch (err: any) {
      setMessages(prev => [
        ...prev,
        {
          id: Math.random().toString(),
          sender: "bot",
          text: `❌ **Error:** ${err?.message || "Failed to communicate with AI agent."}`,
          timestamp: new Date(),
          status: "FAILED"
        }
      ]);
    } finally {
      setIsLoading(false);
    }
  };

  const handleConfirm = (confirmed: boolean) => {
    handleSendMessage(confirmed ? "Yes, confirm" : "No, cancel", true);
  };

  return (
    <>
      {/* Floating Sparkles Bubble Button */}
      <motion.button
        whileHover={{ scale: 1.1 }}
        whileTap={{ scale: 0.95 }}
        onClick={() => setIsOpen(true)}
        className="fixed bottom-24 right-6 z-40 w-14 h-14 rounded-full bg-gradient-to-r from-primary to-indigo-600 text-primary-foreground shadow-lg flex items-center justify-center border border-primary/20 cursor-pointer focus:outline-none focus:ring-2 focus:ring-primary/40 group overflow-hidden"
      >
        {/* Glow pulsing effect */}
        <span className="absolute inset-0 rounded-full bg-primary/20 animate-ping group-hover:animate-none opacity-75"></span>
        <Sparkles className="w-6 h-6 animate-pulse group-hover:rotate-12 transition-transform" />
      </motion.button>

      {/* Slide-over Chat Panel */}
      <AnimatePresence>
        {isOpen && (
          <>
            {/* Backdrop */}
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 0.4 }}
              exit={{ opacity: 0 }}
              onClick={() => setIsOpen(false)}
              className="fixed inset-0 bg-black z-40 backdrop-blur-sm"
            />

            {/* Chat Container */}
            <motion.div
              initial={{ y: "100%", opacity: 0 }}
              animate={{ y: 0, opacity: 1 }}
              exit={{ y: "100%", opacity: 0 }}
              transition={{ type: "spring", damping: 25, stiffness: 250 }}
              className="fixed bottom-0 left-0 right-0 md:left-auto md:right-6 md:top-20 md:bottom-6 md:w-[420px] bg-surface border-t md:border border-border md:rounded-3xl shadow-2xl flex flex-col z-50 overflow-hidden h-[85vh] md:h-[650px] max-h-screen"
            >
              {/* Header */}
              <div className="p-4 bg-gradient-to-r from-primary/10 to-indigo-600/10 border-b border-border flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-2xl bg-gradient-to-r from-primary to-indigo-600 flex items-center justify-center text-primary-foreground shadow-md">
                    <Sparkles className="w-5 h-5 animate-pulse" />
                  </div>
                  <div>
                    <h3 className="font-bold text-foreground text-sm flex items-center gap-1.5">
                      PayVault AI Assistant
                      <span className="w-2 h-2 rounded-full bg-success animate-pulse" />
                    </h3>
                    <p className="text-[11px] text-muted-foreground">Spring AI • Gemini Orchestrator</p>
                  </div>
                </div>
                <button
                  onClick={() => setIsOpen(false)}
                  className="w-8 h-8 rounded-full hover:bg-accent/80 flex items-center justify-center text-muted-foreground hover:text-foreground transition-colors"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>

              {/* Chat History */}
              <div className="flex-1 overflow-y-auto p-4 space-y-4 bg-background/50">
                {messages.map((msg) => (
                  <div
                    key={msg.id}
                    className={`flex gap-3 max-w-[85%] ${
                      msg.sender === "user" ? "ml-auto flex-row-reverse" : ""
                    }`}
                  >
                    {/* Icon */}
                    <div
                      className={`w-8 h-8 rounded-xl flex items-center justify-center shrink-0 shadow-sm border ${
                        msg.sender === "user"
                          ? "bg-surface border-border text-foreground"
                          : "bg-primary/10 border-primary/20 text-primary"
                      }`}
                    >
                      {msg.sender === "user" ? <User size={16} /> : <Bot size={16} />}
                    </div>

                    {/* Bubble Content */}
                    <div className="space-y-1.5">
                      <div
                        className={`rounded-2xl px-4 py-2.5 text-sm leading-relaxed shadow-sm whitespace-pre-wrap ${
                          msg.sender === "user"
                            ? "bg-primary text-primary-foreground rounded-tr-none"
                            : "bg-surface border border-border text-foreground rounded-tl-none"
                        }`}
                      >
                        {msg.text}
                      </div>

                      {/* Tool / Status Metadata Badges */}
                      {(msg.tool || msg.status) && (
                        <div className="flex flex-wrap gap-1.5">
                          {msg.tool && (
                            <span className="text-[10px] font-semibold px-2 py-0.5 rounded-full bg-primary/10 border border-primary/20 text-primary">
                              🛠️ {msg.tool}
                            </span>
                          )}
                          {msg.status && (
                            <span
                              className={`text-[10px] font-semibold px-2 py-0.5 rounded-full border ${
                                msg.status === "SUCCESS"
                                  ? "bg-success/10 border-success/20 text-success"
                                  : msg.status === "FAILED"
                                  ? "bg-danger/10 border-danger/20 text-danger"
                                  : "bg-warning/10 border-warning/20 text-warning"
                              }`}
                            >
                              {msg.status === "SUCCESS" ? "✓ SUCCESS" : msg.status}
                            </span>
                          )}
                        </div>
                      )}
                    </div>
                  </div>
                ))}

                {/* Loading indicator */}
                {isLoading && (
                  <div className="flex gap-3 max-w-[85%]">
                    <div className="w-8 h-8 rounded-xl bg-primary/10 border border-primary/20 text-primary flex items-center justify-center shrink-0 shadow-sm animate-pulse">
                      <Bot size={16} />
                    </div>
                    <div className="bg-surface border border-border rounded-2xl rounded-tl-none px-4 py-3 text-sm flex items-center gap-2 text-muted-foreground shadow-sm">
                      <Loader2 className="w-4 h-4 animate-spin text-primary" />
                      Agent thinking...
                    </div>
                  </div>
                )}

                <div ref={messagesEndRef} />
              </div>

              {/* Bottom Action Footer */}
              <div className="p-4 border-t border-border bg-surface">
                {isConfirmationPending ? (
                  /* Confirmation buttons instead of input box */
                  <div className="space-y-3">
                    <div className="flex items-center gap-2 p-3 rounded-2xl bg-warning/10 border border-warning/20 text-warning text-xs font-semibold">
                      <AlertCircle className="w-4 h-4 shrink-0" />
                      Please confirm execution of this transaction.
                    </div>
                    <div className="grid grid-cols-2 gap-3">
                      <Button
                        onClick={() => handleConfirm(true)}
                        disabled={isLoading}
                        className="bg-success hover:bg-success/90 text-white rounded-xl shadow-md"
                      >
                        Yes, Confirm
                      </Button>
                      <Button
                        onClick={() => handleConfirm(false)}
                        disabled={isLoading}
                        variant="destructive"
                        className="rounded-xl shadow-md"
                      >
                        Cancel
                      </Button>
                    </div>
                  </div>
                ) : (
                  /* Standard Input Box */
                  <form
                    onSubmit={(e) => {
                      e.preventDefault();
                      handleSendMessage(input);
                    }}
                    className="flex items-center gap-2"
                  >
                    <input
                      type="text"
                      value={input}
                      onChange={(e) => setInput(e.target.value)}
                      placeholder="Ask AI... (e.g. 'check balance')"
                      disabled={isLoading}
                      className="flex-1 bg-background border border-border rounded-xl px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary/40 disabled:opacity-50 text-foreground"
                    />
                    <Button
                      type="submit"
                      size="icon"
                      disabled={!input.trim() || isLoading}
                      className="rounded-xl shadow-md h-10 w-10 shrink-0"
                    >
                      <Send className="w-4 h-4" />
                    </Button>
                  </form>
                )}
              </div>
            </motion.div>
          </>
        )}
      </AnimatePresence>
    </>
  );
}
