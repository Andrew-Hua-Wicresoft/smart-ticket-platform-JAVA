import ReactMarkdown from 'react-markdown';

interface MarkdownContentProps {
  content: string;
  maxHeight?: number;
}

export default function MarkdownContent({ content, maxHeight }: MarkdownContentProps) {
  return (
    <div
      style={{
        color: '#595959',
        fontSize: 13,
        lineHeight: 1.75,
        maxHeight,
        overflowY: maxHeight ? 'auto' : undefined,
      }}
    >
      <ReactMarkdown
        components={{
          h1: ({ children }) => (
            <h3 style={{ fontSize: 17, fontWeight: 600, margin: '16px 0 8px', color: '#262626' }}>{children}</h3>
          ),
          h2: ({ children }) => (
            <h4 style={{ fontSize: 15, fontWeight: 600, margin: '14px 0 6px', color: '#262626' }}>{children}</h4>
          ),
          h3: ({ children }) => (
            <h5 style={{ fontSize: 14, fontWeight: 600, margin: '12px 0 4px', color: '#722ed1' }}>{children}</h5>
          ),
          p: ({ children }) => <p style={{ margin: '6px 0' }}>{children}</p>,
          ul: ({ children }) => <ul style={{ paddingLeft: 20, margin: '6px 0' }}>{children}</ul>,
          ol: ({ children }) => <ol style={{ paddingLeft: 20, margin: '6px 0' }}>{children}</ol>,
          li: ({ children }) => <li style={{ marginBottom: 4 }}>{children}</li>,
          blockquote: ({ children }) => (
            <blockquote
              style={{
                borderLeft: '3px solid #d3adf7',
                color: '#595959',
                margin: '8px 0',
                paddingLeft: 12,
              }}
            >
              {children}
            </blockquote>
          ),
          code: ({ children, className }) => {
            const isBlock = className?.includes('language-');
            return (
              <code
                style={{
                  background: '#f5f5f5',
                  borderRadius: 4,
                  color: isBlock ? '#262626' : '#d4380d',
                  display: isBlock ? 'block' : 'inline',
                  fontSize: 12,
                  padding: isBlock ? 12 : '1px 4px',
                  whiteSpace: isBlock ? 'pre-wrap' : undefined,
                }}
              >
                {children}
              </code>
            );
          },
          pre: ({ children }) => (
            <pre style={{ background: '#f5f5f5', borderRadius: 6, margin: '8px 0', overflow: 'auto' }}>
              {children}
            </pre>
          ),
          hr: () => <hr style={{ border: 'none', borderTop: '1px solid #f0f0f0', margin: '12px 0' }} />,
        }}
      >
        {content}
      </ReactMarkdown>
    </div>
  );
}
